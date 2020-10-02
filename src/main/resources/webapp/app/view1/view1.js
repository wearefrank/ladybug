'use strict';

angular.module('myApp.view1', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view1', {
            templateUrl: 'view1/view1.html',
            controller: 'View1Ctrl'
        });
    }])

    .controller('View1Ctrl', ['$scope', '$http', function ($scope, $http) {
        $scope.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
        $scope.columns = ['storageId', 'endTime', 'duration', 'name', 'correlationId', 'status', 'nrChpts', 'estMemUsage', 'storageSize'];
        $scope.storage = "logStorage";
        $scope.treeData = [];
        $scope.reports = [];
        $scope.reportDetails = {text: "INIT", values: {"init": "a", "b": "extra data"}};

        /*
        * Sends a GET request to the server and updates the tables ($scope.metadatas)
        */
        $scope.updateTable = function (data = {}, limit = 10) {
            if (!("limit" in data)) {
                data["limit"] = limit;
            }
            $http.get($scope.apiUrl + "/metadata/" + $scope.storage, {params: data}).then(function (response) {
                let fields = response.data["fields"];
                let values = response.data["values"];
                $scope.metadatas = [];
                values.forEach(function (element) {
                    if (fields.length !== element.length) {
                        return;
                    }
                    let row = {};
                    for (let i = 0; i < fields.length; i++) {
                        row[fields[i]] = element[i];
                    }
                    console.log(row);
                    $scope.metadatas.push(row);
                });
                console.log(response);
            }, function (response) {
                console.error(response);
            });
        };

        /*
         * Refreshes the content of the table, including getting new columns
         */
        $scope.refresh = function () {
            $http.get($scope.apiUrl + '/metadata').then(function (response) {
                $scope.columns = response.data;
                console.log(response);
                $scope.updateTable();
            }, function (response) {
                console.error(response);
            });
        };

        /*
         * Displays a selected reports in the content pane.
         */
        $scope.display_report = function (event, node) {
            // Todo: display on the other column.
            console.log("EVENT SELECTED " + node.nodeId);
            console.log(event);
            console.log(node);
            let ladybugData = node["ladybug"];

            $scope.reportDetails = {
                text: ladybugData["message"],
                values: {
                    "Name:": ladybugData["name"],
                    "Thread name:": ladybugData["threadName"],
                    "Source class name:": ladybugData["sourceClassName"],
                    "Path:": "???",
                    "Checkpoint UID:": ladybugData["uid"],
                    "Number of characters:": "???",
                    "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
                }
            };
            console.log($scope.reportDetails);
        };

        $scope.remove_circulars = function (node) {
            if ("parent" in node) {
                delete node["parent"];
            }
            if ("nodes" in node) {
                for (let i = 0; i < node["nodes"].length; i++) {
                    $scope.remove_circulars(node["nodes"][i]);
                }
            }
        };

        $scope.selectReport = function (report) {
            console.log("CLICK");
            console.log(report);
            $http.get($scope.apiUrl + "/report/" + $scope.storage + "/" + report["storageId"])
                .then(function (response) {
                    console.log(response);
                    console.log(response.data);
                    $scope.reports[response.data["storageId"]] = response.data;
                    let report = {text: response.data["name"], icon: "fa fa-file-code", nodes: []};
                    let checkpoints = response.data["checkpoints"];
                    let nodes = report.nodes;
                    let previous_node = null;
                    for (let i = 0; i < checkpoints.length; i++) {
                        let chkpt = checkpoints[i];
                        let node = {text: chkpt["name"], ladybug: chkpt, level: chkpt["level"]};

                        if (previous_node === null) {
                            nodes.push(node);
                            previous_node = node;
                            console.log(" -- Added: " + node.text);
                            continue;
                        }
                        let diff = (node.level - 1) - previous_node.level;

                        while (diff !== 0) {
                            console.log("aim: " + node.level);
                            console.log("prev: " + previous_node.level);
                            console.log("Prev Text: " + previous_node.text);
                            console.log("diff: " + diff);
                            if (diff < 0) {
                                previous_node = previous_node.parent;
                            } else {
                                if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                                    break;
                                }
                                previous_node = previous_node.nodes[previous_node.nodes - 1];
                            }
                            diff = (node.level - 1) - previous_node.level;
                        }
                        if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                            previous_node["nodes"] = [];
                        }
                        previous_node["nodes"].push(node);
                        node.parent = previous_node;
                        previous_node = node;
                        console.log(" -- Added: " + node.ladybug["index"] + " " + node.text);
                        console.log(report);
                    }
                    $scope.remove_circulars(report);
                    $scope.treeData.push(report);
                    console.log($scope.treeData);
                    // Update tree
                    $('#tree').treeview({
                        data: $scope.treeData,
                        onNodeSelected: $scope.display_report,
                        onNodeUnselected: function (event, node) {
                            console.log("UNSELECTED");
                            console.log(event);
                            console.log(node);
                            $scope.reportDetails = {text: "", values: {}};
                        }
                    });
                    console.log("END")
                }, function (response) {
                    console.error(response);
                });

        };
        $scope.refresh();
    }]);