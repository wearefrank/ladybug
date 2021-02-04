'use strict';

angular.module('myApp.view3', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/view3', {
    templateUrl: 'views/compare/view3.html',
    controller: 'View3Ctrl'
  });
}])

.controller('View3Ctrl', ['$scope', function($scope) {
  $scope.left_table2tree = {};
  $scope.left_tree2display = {};
  $scope.right_table2tree = {};
  $scope.right_tree2display = {};
  $scope.left_show = true;
  $scope.right_show = true;

  $scope.left_tree2display.select = function (rootNode, event, node) {
      $scope.left_show = node === null;
    $scope.$apply();
  };
  $scope.right_tree2display.select = function (rootNode, event, node) {
    $scope.right_show = node === null;
    $scope.$apply();
  };
}]);