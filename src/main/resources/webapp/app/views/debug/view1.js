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
        $scope.storage = "debugStorage";
        $scope.table2tree = {};
        $scope.tree2display = {};
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
    }]);