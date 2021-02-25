'use strict';

angular.module('myApp.view1', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view1', {
            templateUrl: 'views/debug/view1.html',
            controller: 'View1Ctrl'
        });
    }])

    .controller('View1Ctrl', ['$scope', '$http', function ($scope, $http) {
        $scope.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
        $scope.columns = ['storageId', 'endTime', 'duration', 'name', 'correlationId', 'status', 'nrChpts', 'estMemUsage', 'storageSize'];
        $scope.storage = "debugStorage";
        $scope.table2tree = {};
        $scope.tree2display = {};
        $scope.limit = 5;
        $scope.filters = {"limit": $scope.limit};
        $scope.options = {
            "generatorEnabled": true,
            "regexFilter": "^(?!Pipeline WebControl).*",
            "estMemory": 0,
            "reportsInProgress": 0,
            "transformation": "",
            "transformationEnabled": true,
            "openLatest": 10,
            "openInProgress": 1
        };

        $scope.updateOptions = function() {
            $http.get($scope.apiUrl + "/testtool")
                .then(function (response) {
                    $scope.options = Object.assign($scope.options, response.data);
                    $scope.testtoolStubStrategies = response.data["stubStrategies"];
                    $scope.stubStrategies = $scope.testtoolStubStrategies;
                    console.log("/testtool");
                    console.log(response);
                    console.log($scope.options);
                }, function (response) {
                    console.log("/testtool failed");
                });
            $http.get($scope.apiUrl + "/testtool/transformation")
                .then(function (response) {
                    $scope.options = Object.assign($scope.options, response.data);
                    $scope.options['transformationEnabled'] = $scope.options['transformation'] !== "";
                    console.log("/testtool/transformation");
                });
        }

        $scope.saveOptions = function() {
            console.log("Saving Options");
            console.log($scope.options);
            $http.post($scope.apiUrl + "/testtool", {
                "generatorEnabled": $scope.options['generatorEnabled'],
                "regexFilter": $scope.options["regexFilter"]});
            $http.post($scope.apiUrl + "/testtool/transformation", {"transformation": $scope.options['transformation']});
        }

        $scope.openLatestReports = function (number) {
            $http.get($scope.apiUrl + "/report/latest/" + $scope.storage + "/" + number)
                .then(function (response) {
                    response.data.forEach(function (report) {
                        $scope.table2tree.add(report);
                        console.log(report);
                    });
                });
        }
        // TODO: Move
        // $scope.deleteSelected = function () {
        //     let tree = $('#tree').treeview(true);
        //     if (tree.getSelected().length === 0)
        //         return;
        //     let node = tree.getSelected()[0];
        //     while (node.parentId !== undefined) {
        //         node = tree.getNode(node.parentId);
        //     }
        //     console.log("DELETE");
        //     console.log(node["ladybug"]["storageId"]);
        //     $http.delete($scope.apiUrl + "/report/" + $scope.storage + "/" + node["ladybug"]["storageId"])
        //         .then(function () {
        //             tree.remove(node);
        //         }, function (response) {
        //             console.log(response);
        //             alert(response.data);
        //         });
        // }

        $scope.updateOptions();
    }]);