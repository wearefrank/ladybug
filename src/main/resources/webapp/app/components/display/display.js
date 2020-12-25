function displayController($scope) {
    let ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.reportDetails = {text: "asdasd", values: {}};
    ctrl.stubStrategySelect = "";
    ctrl.availableStorages = {'runStorage': 'Test'};

    ctrl.rerun = function () {
        let data = {};
        data[$scope.storage] = [ctrl.selectedNode["ladybug"]["storageId"]];
        console.log("RERUN");
        console.log(data);
        $http.post(ctrl.apiUrl + "/runner/run/" + ctrl.storage, data)
            .then(function (response) {
                console.log(response);
            }, function (response) {
                console.log(response);
            });
    };

    ctrl.toggleEdit = function () {
        console.log("toggling");
        let codeWrappers = $('#code-wrapper');
        let htmlText = '<pre id="code-wrapper" class="prettify"><code id="code" class="lang-xml"></code></pre>';
        let buttonText = 'Edit';

        if (codeWrappers.get().length === 0 || codeWrappers.get()[0].tagName === "PRE") {
            let rows = Math.min(20, ctrl.reportDetails.text.split('\n').length);
            htmlText = '<div id="code-wrapper" class=\"form-group\"><textarea class=\"form-control\" id=\"code\" rows="' + rows + '"></textarea></div>';
            buttonText = 'Save';
        }
        // TODO: Save the text|
        codeWrappers.remove();
        $('#details-edit').text(buttonText);
        $('#details-row').after(htmlText);
        $('#code').text(ctrl.reportDetails.text);
    }

    ctrl.copyReport = function (to) {
        let data = {};
        data[ctrl.storage] = [ctrl.selectedNode["ladybug"]["storageId"]];
        $http.put(ctrl.apiUrl + "/report/store/" + to, data);
    }

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        queryString = "?id=" + ctrl.selectedNode["ladybug"]["storageId"] + "";
        window.open(ctrl.apiUrl + "/report/download/" + ctrl.storage + "/" + exportReport + "/" +
            exportReportXml + queryString);
    }

    /*
     * Displays a selected reports in the content pane.
     */
    ctrl.display_report = function (rootNode, event, node) {
        ctrl.selectedNode = rootNode;
        if (node === null) {
            ctrl.reportDetails = {text: "", values: {}};
            return;
        }
        let ladybugData = node["ladybug"];
        $('#code-wrapper').remove();
        $('#details-row').after('<pre id="code-wrapper" class="prettify"><code id="code" class="lang-xml"></code></pre>');
        $('#code').text(ladybugData["message"]);
        console.log("ladybugData");
        console.log(ladybugData["message"]);
        ctrl.reportDetails = {
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
            ctrl.stubStrategies = ["Follow report strategy", "No", "Yes"];
            ctrl.stubStrategySelect = ctrl.stubStrategies[stubStrategySelect + 1];
        } else {
            // If node is report
            ctrl.stubStrategies = $scope.testtoolStubStrategies;
            ctrl.stubStrategySelect = stubStrategySelect;
        }
        $scope.$apply();
        PR.prettyPrint()
    };

    ctrl.$onInit = function () {
        ctrl.onSelectRelay.select = ctrl.display_report;
        ctrl.stubStrategies = $scope.testtoolStubStrategies;
    }
}

angular.module('myApp').component('reportDisplay', {
    templateUrl: 'components/display/display.html',
    controller: ['$scope', displayController],
    bindings: {
        onSelectRelay: '=',
    }
});