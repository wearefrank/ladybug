/*
   Copyright 2026 WeAreFrank!, 2018 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.wearefrank.ladybug.web.common.shownreport;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.Report;
import org.wearefrank.ladybug.filter.View;
import org.wearefrank.ladybug.web.common.HttpInternalServerErrorException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class ShownReportBuilder {

	private static abstract class Ancestor {
		final TreeNode contents;

		Ancestor(TreeNode contents) {
			this.contents = contents;
		}

		// Returns true if the white box view would show the argument checkpoint
		// inside the wrapped checkpoint or report. The end node corresponding to a start
		// node is shown inside that start node.
		//
		// This method expects to see all checkpoints of a report. When it returns
		// false then this Ancestor should be removed from the stack maintained
		// by inner class Session.
		abstract boolean acceptChild(ShownCheckpoint checkpoint) throws HttpInternalServerErrorException;

		abstract boolean isShown();
	}

	private static final class ReportAncestor extends Ancestor {
		ReportAncestor(ShownReport report) {
			super(report);
		}

		@Override
		boolean acceptChild(ShownCheckpoint checkpoint) {
			return true;
		}

		@Override
		boolean isShown() {
			return true;
		}
	}

	private static final class CheckpointAncestor extends Ancestor {
		private boolean open = false;
		private final int level;
		private boolean shown;

		CheckpointAncestor(ShownCheckpoint checkpoint, boolean shown) {
			super(checkpoint);
			this.level = checkpoint.getLevel();
			this.shown = shown;
		}

		@Override
		boolean isShown() {
			return shown;
		}

		@Override
		boolean acceptChild(ShownCheckpoint checkpoint) throws HttpInternalServerErrorException {
			if (checkpoint.getLevel() > level) {
				open = true;
				return true;
			} else if (checkpoint.getLevel() == level) {
				if (open) {
					open = false;
					return true;
				} else {
					return false;
				}
			} else {
				throw new HttpInternalServerErrorException("Expected that ancestor was removed");
			}
		}
	}


	private static final class Session {
		private Deque<Ancestor> ancestors = new ArrayDeque<>();
		ShownReport result;

		Session(ShownReport report) {
			result = report;
			ancestors.addLast(new ReportAncestor(report));
		}

		void handleCheckpoint(ShownCheckpoint checkpoint, boolean shown) throws HttpInternalServerErrorException {
			Ancestor ancestor = ancestors.getLast();
			while (!ancestor.acceptChild(checkpoint)) {
				ancestors.removeLast();
				ancestor = ancestors.getLast();
			}
			if (shown) {
				showChild(checkpoint);
			}
			ancestors.addLast(new CheckpointAncestor(checkpoint, shown));
		}

		void showChild(ShownCheckpoint checkpoint) {
			Iterator<Ancestor> it = ancestors.descendingIterator();
			while (it.hasNext()) {
				Ancestor shownParent = it.next();
				if (shownParent.isShown()) {
					shownParent.contents.addChild(checkpoint);
					return;
				}
			}
		}
	}

	public ShownReport transform(Report report, View view) throws HttpInternalServerErrorException {
		ShownReport shownReport = new ShownReport();
		copyReport(report, shownReport);
		Session session = new Session(shownReport);
		for (Checkpoint checkpoint: report.getCheckpoints()) {
			boolean shown = view.showCheckpoint(report, checkpoint);
			ShownCheckpoint shownCheckpoint = new ShownCheckpoint();
			copyCheckpoint(checkpoint, shownCheckpoint);
			session.handleCheckpoint(shownCheckpoint, shown);
		}
		return session.result;
	}

	private void copyReport(Report source, ShownReport dest) {
		dest.setName(source.getName());
		dest.setDescription(source.getDescription());
		dest.setPath(source.getPath());
		dest.setStubStrategy(source.getStubStrategy());
		dest.setLinkMethod(source.getLinkMethod());
		dest.setTransformation(source.getTransformation());
		dest.setStorageId(source.getStorageId());
		dest.setStorageName(source.getStorage().getName());
		dest.setEstimatedMemoryUsage(source.getEstimatedMemoryUsage());
		dest.setCorrelationId(source.getCorrelationId());
	}

	private void copyCheckpoint(Checkpoint source, ShownCheckpoint dest) {
		dest.setName(source.getName());
		dest.setMessage(source.getMessage());
		dest.setEncoding(source.getEncoding());
		dest.setMessageContext(source.getMessageContext());
		dest.setType(source.getType());
		dest.setLevel(source.getLevel());
		dest.setStub(source.getStub());
		dest.setStubbed(source.isStubbed());
		dest.setStubNotFound(source.getStubNotFound());
		dest.setPreTruncatedMessageLength(source.getPreTruncatedMessageLength());
		dest.setTypeAsString(source.getTypeAsString());
		dest.setThreadName(source.getThreadName());
		dest.setSourceClassName(source.getSourceClassName());
		dest.setMessageClassName(source.getMessageClassName());
		dest.setUid(source.getUid());
	}
}
