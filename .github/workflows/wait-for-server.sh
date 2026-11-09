#!/usr/bin/env bash
# Waits until a server is healthy, by polling a health check URL until it returns one of the
# accepted HTTP status codes. Fails when that does not happen within the timeout.
#
# Every request has a time limit, so a server that accepts connections but does not answer cannot
# make this script hang. Any other HTTP status code (for example 404 when the application is not
# deployed, or 503 when it is not healthy) means that the server is not healthy (yet).
#
# When the server does not become healthy, the response of the health check URL is shown, which
# usually tells what is wrong.
#
# Usage: wait-for-server.sh <url> <timeout-in-seconds> [<accepted-status-code> ...]
# Without accepted status codes, only 200 is accepted.
set -uo pipefail

url="$1"
timeout_seconds="$2"
shift 2
accepted_status_codes=("$@")
if [[ ${#accepted_status_codes[@]} -eq 0 ]]; then
  accepted_status_codes=(200)
fi

# Time limit of a single request.
request_max_seconds=10
# Time between requests.
interval_seconds=5
# Time between progress messages.
progress_interval_seconds=30

is_accepted() {
  local status="$1"
  local accepted
  for accepted in "${accepted_status_codes[@]}"; do
    if [[ "${status}" == "${accepted}" ]]; then
      return 0
    fi
  done
  return 1
}

echo "Waiting at most ${timeout_seconds} seconds until ${url} returns HTTP status ${accepted_status_codes[*]}"
start_time=${SECONDS}
deadline=$((start_time + timeout_seconds))
next_progress_time=$((start_time + progress_interval_seconds))
while true; do
  # Status 000 means that there was no HTTP response, for example because the connection was
  # refused or the request took longer than its time limit.
  status="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time "${request_max_seconds}" "${url}")"
  if is_accepted "${status}"; then
    echo "Server is healthy after $((SECONDS - start_time)) seconds: ${url} returned HTTP status ${status}"
    exit 0
  fi
  if [[ ${SECONDS} -ge ${deadline} ]]; then
    break
  fi
  if [[ ${SECONDS} -ge ${next_progress_time} ]]; then
    echo "Still waiting after $((SECONDS - start_time)) seconds, last HTTP status ${status}"
    next_progress_time=$((SECONDS + progress_interval_seconds))
  fi
  sleep "${interval_seconds}"
done

echo "Server is not healthy after ${timeout_seconds} seconds, last HTTP status ${status}. Response of ${url}:" >&2
curl --silent --show-error --include --max-time "${request_max_seconds}" "${url}" >&2
echo >&2
exit 1
