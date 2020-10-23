'use strict';

angular.module('myApp.view2', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view2', {
            templateUrl: 'view2/view2.html',
            controller: 'View2Ctrl'
        });
    }])

    .controller('View2Ctrl', ['$scope', '$http', function ($scope, $http) {
        $scope.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
        $scope.storage = 'runStorage';
        $scope.moveTo = "/";
        $scope.showOptions = {ids: false, checkpoints: false};
        $scope.reports = [];
        $scope.treeData = [{}];
        $scope.cloneInputs = {csv: "", message: "", storageId: "", force: false};

        $scope.addNode = function (rootNode, path, data) {
            console.log("INSIDE ADDNODE");
            console.log({path: path, data: data, rootnode: rootNode});
            if (path === undefined || path.length === 0 || (path.length === 1 && path[0] === "")) {
                rootNode["ladybug"].push(data);
                return;
            }

            let nodes = rootNode["nodes"];
            for (let i = 0; i < nodes.length; i++) {
                if (nodes[i]["path"] === path[0]) {
                    $scope.addNode(nodes[i], path.slice(1), data);
                    return;
                }
            }
            let node = {path: path[0], text: path[0], icon: "fa fa-file-code", ladybug: [], nodes: []};
            nodes.push(node);
            $scope.addNode(node, path.slice(1), data);
        };

        $scope.openNode = function (node, checked) {
            console.log(node);
            console.log(checked)
            let reports = node["ladybug"];
            for (let i = 0; i < reports.length; i++) {
                reports[i]["checked"] = checked;
                reports[i]["csvText"] = $scope.csv2text(reports[i]["variableCsv"]);
                $scope.reports.push(reports[i]);
            }
            let nodes = node["nodes"];
            for (let i = 0; i < nodes.length; i++) {
                $scope.openNode(nodes[i], false);
            }
        };

        $scope.getSelectedReports = function () {
            let reports = [];
            for (let i = 0; i < $scope.reports.length; i++) {
                if ($scope.reports[i]["checked"] === true)
                    reports.push($scope.reports[i]);
            }
            return reports;
        }

        $scope.openReport = function (reports) {
        };

        $scope.runReport = function () {
            let reports = $scope.getSelectedReports();
            let data = {};
            data[$scope.storage] = [];
            for (let i = 0; i < reports.length; i++) {
                data[$scope.storage].push(reports[i]["storageId"]);
            }
            $http.post($scope.apiUrl + "/runner/run/logStorage", data)
                .then(function (response) {
                    console.log("RUN RETURN");
                    console.log(response);
                }, function (response) {
                    console.error(response);
                })
        };

        $scope.treeSelect = function () {
            let nodes = $('#tree').treeview('getSelected');
            $scope.reports = [];
            for (let i = 0; i < nodes.length; i++) {
                $scope.openNode(nodes[i], true);
            }
            $scope.$apply();
        };

        $scope.updateTree = function () {
            $http.get($scope.apiUrl + "/metadata/" + $scope.storage)
                .then(function (response) {
                    let fields = response.data["fields"];
                    let values = response.data["values"];
                    console.log(response.data);
                    let rootNode = {
                        path: "/",
                        text: "Reports",
                        icon: "fa fa-file-code",
                        ladybug: [],
                        nodes: [],
                        state: {selected: true}
                    };
                    for (let i = 0; i < values.length; i++) {
                        let data = {};
                        for (let v = 0; v < fields.length; v++) {
                            data[fields[v]] = values[i][v];
                        }

                        let path = data["path"];
                        if (path.startsWith("/"))
                            path = path.slice(1);
                        if (path.endsWith("/"))
                            path = path.slice(0, -1);
                        console.log(path);
                        console.log(path.split("/"));
                        $scope.addNode(rootNode, path.split("/"), data);
                    }

                    $scope.treeData = [rootNode];
                    $('#tree').treeview({
                        data: $scope.treeData,
                        onNodeSelected: $scope.treeSelect
                    });
                    $scope.treeSelect();
                }, function (response) {
                    console.error(response);
                });
        };

        $scope.updateReports = function () {
        };

        $scope.uploadReport = function () {
            let files = document.getElementById("upload-file").files;
            if (files.length === 0)
                return;
            for (let i = 0; i < files.length; i++) {
                let formdata = new FormData();
                formdata.append("file", files[i]);
                $http.post($scope.apiUrl + "/report/upload/" + $scope.storage, formdata, {headers: {"Content-Type": undefined}});
            }
        }

        $scope.deleteReports = function () {
            let reports = $scope.getSelectedReports();
            for (let i = 0; i < reports.length; i++) {
                $http.delete($scope.apiUrl + "/report/" + $scope.storage + "/" + reports["storageId"]);
            }
        }

        $scope.moveReports = function (reports, action) {
            let path = $('#moveToInput').val();
            console.log(action + " to " + path);
            for (let i = 0; i < reports.length; i++) {
                $http.put($scope.apiUrl + "/report/move/" + $scope.storage + "/" + reports[i]["storageId"],
                    {path: path, action: action}).then(function (response) {
                    $scope.refresh();
                });
            }
        };

        $scope.openCloneModal = function (reports) {
            let storageId = reports[0]["storageId"]
            $http.get($scope.apiUrl + "/report/" + $scope.storage + "/" + storageId)
                .then(function (response) {
                    $scope.cloneInputs.message = response.data["inputCheckpoint"]["message"];
                    $scope.cloneInputs.storageId = storageId;
                    $('#cloneModal').modal('show');
                }, function (response) {
                    alert("couldnt get report!");
                    console.log(response);
                })
        }

        $scope.closeCloneModal = function () {
            $scope.cloneInputs = {csv: "", message: "", storageId: "", force: false};
            $scope.refresh();
        }

        $scope.cloneReport = function () {
            $http.post($scope.apiUrl + "/report/clone/" + $scope.storage + "/" + $scope.cloneInputs.storageId,
                {
                    csv: $scope.cloneInputs.csv,
                    message: $scope.cloneInputs.message,
                    force: $scope.cloneInputs.force
                }).then($scope.closeCloneModal, function (response) {
                $scope.cloneInputs.force = true;
                alert("couldnt clone it");
                console.log(response);
            })
        }

        $scope.csv2text = function (csv) {
            if (csv === undefined || csv === "")
                return "";

            let lines = csv.split("\n");
            if (lines.length !== 2)
                return "";

            let keys = lines[0].split(";");
            let values = lines[1].split(";");
            if (keys.length !== values.length)
                return "";
            let str = "";
            for (let i = 0; i < keys.length; i++) {
                str += ", " + keys[i].trim() + " = " + values[i].trim();
            }
            return str.slice(2);
        }

        $scope.resetRunner = function () {
            $http.post($scope.apiUrl + "/runner/reset");
        }

        $scope.changeSelectedAll = function (value) {
            for (let i = 0; i < $scope.reports.length; i++) {
                $scope.reports[i]["checked"] = value;
            }
        };

        $scope.refresh = function () {
            $scope.updateTree();
            $scope.updateReports();
        }

        $('#cloneModal').on('hidden.bs.modal', $scope.closeCloneModal)
        $scope.refresh();
    }]);