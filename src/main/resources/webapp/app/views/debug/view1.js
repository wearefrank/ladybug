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
        $scope.reportDetails = {text: "", values: {}};
        $scope.table2tree = {};
        $scope.limit = 5;
        $scope.filters = {"limit": $scope.limit};
        $scope.options = {
            "generatorEnabled": true,
            "regexFilter": "^(?!Pipeline WebControl).*",
            "estMemory": 0,
            "reportsInProgress": 0,
            "transformation": "",
            "transformationEnabled": false,
            "openLatest": 10,
            "openInProgress": 1
        };
        $scope.availableStorages = {'runStorage': 'Test'};
        $scope.stubStrategySelect = "";

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

        /*
         * Displays a selected reports in the content pane.
         */
        $scope.display_report = function (rootNode, event, node) {
            $scope.selectedNode = rootNode;
            if (node === null) {
                $scope.reportDetails = {text: "", values: {}};
                return;
            }
            let ladybugData = node["ladybug"];
            $('#code-wrapper').remove();
            $('#details-row').after('<pre id="code-wrapper" class="prettify"><code id="code" class="lang-xml"></code></pre>');
            $('#code').text(ladybugData["message"]);
            console.log("ladybugData");
            console.log(ladybugData["message"]);
            $scope.reportDetails = {
                data: ladybugData,
                text: ladybugData["message"],
                values: {
                    "Name:": ladybugData["name"],
                    "Thread name:": ladybugData["threadName"],
                    "Source class name:": ladybugData["sourceClassName"],
                    "Path:": "???",
                    "Checkpoint UID:": ladybugData["uid"],
                    "Number of characters:": "???",
                    "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
                },
                nodeId: node.nodeId
            };
            let stubStrategySelect = ladybugData["stubStrategy"];
            if (stubStrategySelect === undefined) {
                // If node is checkpoint
                stubStrategySelect = ladybugData["stub"];
                $scope.stubStrategies = ["Follow report strategy", "No", "Yes"];
                $scope.stubStrategySelect = $scope.stubStrategies[stubStrategySelect + 1];
            } else {
                // If node is report
                $scope.stubStrategies = $scope.testtoolStubStrategies;
                $scope.stubStrategySelect = stubStrategySelect;
            }
            $scope.$apply();
            PR.prettyPrint()
        };
        $scope.logg = function () {
            console.log($scope.filters);
        }

        $scope.toggleEdit = function () {
            console.log("toggling");
            let codeWrappers = $('#code-wrapper');
            let htmlText = '<pre id="code-wrapper" class="prettify"><code id="code" class="lang-xml"></code></pre>';
            let buttonText = 'Edit';

            if (codeWrappers.get().length === 0 || codeWrappers.get()[0].tagName === "PRE") {
                let rows = Math.min(20, $scope.reportDetails.text.split('\n').length);
                htmlText = '<div id="code-wrapper" class=\"form-group\"><textarea class=\"form-control\" id=\"code\" rows="' + rows + '"></textarea></div>';
                buttonText = 'Save';
            }
            // TODO: Save the text|
            codeWrappers.remove();
            $('#details-edit').text(buttonText);
            $('#details-row').after(htmlText);
            $('#code').text($scope.reportDetails.text);
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

        $scope.collapse = function () {
            $('#tree').treeview('collapseNode', $scope.reportDetails.nodeId);
        }
        $scope.expand = function () {
            let tree = $('#tree').treeview(true);
            let node = tree.getNode($scope.reportDetails.nodeId);
            let path = [];
            path.push(node.nodeId);
            while (node.parentId !== undefined) {
                node = tree.getNode(node.parentId);
                path.push(node.nodeId);
            }
            console.log("Expanding");
            console.log(path);
            for (let i = path.length - 1; i >= 0; i--) {
                tree.expandNode(path[i]);
            }
        }

        $scope.rerun = function () {
            let rootNode = $scope.getRootNode();
            let data = {};
            data[$scope.storage] = [rootNode["ladybug"]["storageId"]];
            console.log("RERUN");
            console.log(data);
            $http.post($scope.apiUrl + "/runner/run/" + $scope.storage, data)
                .then(function (response) {
                    console.log(response);
                }, function (response) {
                    console.log(response);
                });
        };


        $scope.copyReport = function (to) {
            let data = {};
            data[$scope.storage] = [$scope.getRootNode()["ladybug"]["storageId"]];
            $http.put($scope.apiUrl + "/report/store/" + to, data);
        }

        $scope.updateOptions();
    }]);