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
        $scope.showIds = true;
        $scope.reports = [];
        $scope.treeData = [{}];

        $scope.addNode = function (rootNode, path, data) {
            console.log("INSIDE ADDNODE");
            console.log({path: path, data: data, rootnode: rootNode});
            if (path === undefined || path.length === 0 || (path.length === 1 && path[0] === "")) {
                rootNode["ladybug"].push(data);
                return;
            }

            let nodes = rootNode["nodes"];
            for(let i = 0; i < nodes.length; i++) {
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
                $scope.reports.push(reports[i]);
            }
            let nodes = node["nodes"];
            for (let i = 0; i < nodes.length; i++) {
                $scope.openNode(nodes[i], false);
            }
        };

        $scope.openReport = function (reports) {
        };

        $scope.runReport = function (reports) {
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
            for (let i = 0; i < nodes.length; i ++) {
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
                    let rootNode = {path: "/", text: "Reports", icon: "fa fa-file-code", ladybug: [], nodes: []};
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
                }, function (response) {
                    console.error(response);
                });
        };

        $scope.updateReports = function () {
        };

        $scope.changeSelectedAll = function (value) {
            for (let i = 0; i < $scope.reports.length; i++) {
                $scope.reports[i]["checked"] = value;
            }
        };

        $scope.refresh = function () {
            $scope.updateTree();
            $scope.updateReports();
        }

        $scope.refresh();
    }]);