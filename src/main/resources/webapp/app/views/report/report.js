'use strict';

angular.module('myApp.report', ['ngRoute'])

.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/report', {
        templateUrl: 'views/report/report.html',
        controller: 'ReportCtrl'
    });
}])

.controller('ReportCtrl', ['$scope', '$rootScope', '$location', '$http', function ($scope, $rootScope, $location, $http) {
    $scope.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    $rootScope.openedReports = [];
    $rootScope.tabs = ($rootScope.hasOwnProperty("tabs")) ? $rootScope.tabs : {};
    $scope.addRelay = {};
    $scope.tree2display = {};
    $scope.storage = 'testStorage';
    $scope.table2tree = {transformationEnabled: true};

    console.log("Report Controller Started");

    $scope.delete_tab = function (storageId) {
        $("#" + storageId).remove();
        window.location = "#!/view1";
    }

    $scope.add_tab = function (tabName, storageId, storage) {
        $rootScope.tabs[storageId] = tabName;
        $('#ladybug-tabs').append("<li class=\"nav-item active\" id=\"" + storageId + "\"><a href=\"#!/report?storage="
            + storage + "&storageId=" + storageId + "\" " +  "class=\"nav-link active\">" + tabName + "</a></li>");
    }

    $scope.addRelay.postInit = function() {
        console.log("Report Controller oninit");
        let searchObject = $location.search();
        console.log("openreport", searchObject);

        $http.get($scope.apiUrl + "/report/" + searchObject.storage + "/" + searchObject.storageId)
            .then(function (response) {
                console.log("Open Report oninit");
                console.log(response.data);
                $scope.add_tab(response.data.name, response.data.storageId, searchObject.storage);
                $scope.addRelay.add(response.data);
                window.setTimeout(() => {
                    $scope.tree2display.selectPath([0]);
                }, 500);
            }, function (response) {
                console.error(response);
            });
    }
}]);