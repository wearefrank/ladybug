function displayController($scope, $http) {
    let ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.reportDetails = {text: "", values: {}, notifications: {}};
    ctrl.stubStrategySelect = "";
    ctrl.availableStorages = {'runStorage': 'Test'};
    ctrl.displayingReport = false;
    ctrl.id = Math.random().toString(36).substring(7);
    ctrl.editing = false;
    ctrl.diff = [];
    $scope.overwritten_checkpoints = {}; // This will contain keys as checkpoint id, and values as edited values.

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

        if (codeWrappers.get().length === 0 || codeWrappers.get()[0].tagName === "PRE") {
            let htmlText = '<div id="code-wrapper" class=\"form-group\" style="min-height: 400px; min-width: 300px;"></div>';
            let buttonText = 'Read-only';
            codeWrappers.remove();
            $('#details-edit').text(buttonText);
            $('#details-row').after(htmlText);
            require(['vs/editor/editor.main'], function () {
                ctrl.editor = monaco.editor.create(document.getElementById('code-wrapper'), {
                    value: ctrl.reportDetails.text,
                    language: 'xml'
                });
                ctrl.editing = true;
            });
        } else {
            let editedText = (("editor" in ctrl && save) ? ctrl.editor.getValue() : ctrl.reportDetails.text);
            if (editedText !== ctrl.reportDetails.text) {
                let dmp = new diff_match_patch();
                ctrl.diff = dmp.diff_main(ctrl.reportDetails.text, editedText);
                dmp.diff_cleanupSemantic(ctrl.diff);
                console.log("Diff", ctrl.diff);
                $('#modal' + ctrl.id).modal('show');
            } else {
                ctrl.saveEditField(false);
            }
        }
    }

    ctrl.saveEditField = function (save) {
        let codeWrappers = $('#code-wrapper');
        ctrl.reportDetails.text = ((save && "editor" in ctrl) ? ctrl.editor.getValue() : ctrl.reportDetails.text);
        let htmlText = '<pre id="code-wrapper"><code id="code" class="xml"></code></pre>';
        let buttonText = 'Edit';
        // TODO: Save the text
        if ("uid" in ctrl.reportDetails.data)
            $scope.overwritten_checkpoints[ctrl.reportDetails.data.uid] = ctrl.reportDetails.text;
        ctrl.reportDetails.data["message"] = ctrl.reportDetails.text;
        codeWrappers.remove();
        $('#details-edit').text(buttonText);
        $('#details-row').after(htmlText);
        $('#code').text(ctrl.reportDetails.text);
        ctrl.highlight_code();
        ctrl.editing = false;
        $('#modal' + ctrl.id).modal('hide');
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
        $('#code-wrapper').remove();
        if (node === null) {
            ctrl.reportDetails = {text: "", values: {}, notifications: {}};
            return;
        }
        let ladybugData = node["ladybug"];
        $('#details-row').after('<pre id="code-wrapper"><code id="code" class="xml"></code></pre>');

        if (ctrl.isReportNode(node)) {
            // If node is checkpoint
            ctrl.display_checkpoint(ladybugData);
        } else {
            // If node is report
            ctrl.display_report_(ladybugData);
        }
        ctrl.reportDetails.nodeId = node.nodeId;
        if (!$scope.$$phase) $scope.$apply();
    };

    ctrl.display_checkpoint = function (ladybugData) {
        let message = ladybugData["message"];
        if (ladybugData["uid"] in $scope.overwritten_checkpoints)
            message = $scope.overwritten_checkpoints[ladybugData["uid"]];
        ctrl.reportDetails = {
            data: ladybugData,
            text: message,
            values: {
                "Name:": ladybugData["name"],
                "Thread name:": ladybugData["threadName"],
                "Source class name:": ladybugData["sourceClassName"],
                "Checkpoint UID:": ladybugData["uid"],
                "Number of characters:": "???",
                "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
            },
            notifications: {0: {level: "error", text: "I see you are displaying a checkpoint!"}}
        };
        ctrl.stubStrategies = ["Follow report strategy", "No", "Yes"];
        ctrl.stubStrategySelect = ctrl.stubStrategies[ladybugData["stub"] + 1];

        $('#code').text(ctrl.reportDetails.text);
        ctrl.highlight_code();
    };

    ctrl.display_report_ = function (ladybugData) {
        if ("message" in ladybugData) {
            ctrl.reportDetails.text = ladybugData["message"];
            $('#code').text(ladybugData["message"]);
        } else {
            let transformer = "applyTransformer" in ctrl && ctrl["applyTransformer"];
            console.log("URL:" + ctrl.apiUrl + "/report/" + ctrl.storage + "/" + ladybugData.storageId + "?xml=true&globalTransformer=" + transformer);
            $http.get(ctrl.apiUrl + "/report/" + ctrl.storage + "/" + ladybugData.storageId + "?xml=true&globalTransformer=" + transformer)
                .then(function (response) {
                    console.log("Message", response.data);
                    // let reportXml = ctrl.escape_html(response.data["xml"]);
                    let reportXml = response.data["xml"]
                    console.log("XML DATA", reportXml);
                    ctrl.reportDetails.text = reportXml;
                    ladybugData.message = reportXml;
                    $('#code').text(reportXml);
                    ctrl.highlight_code();
                });
        }
        ctrl.reportDetails = {
            data: ladybugData,
            values: {
                "Name:": ladybugData["name"],
                "Path:": ladybugData["path"],
                "Transformation:": ladybugData["transformation"],
                "StorageId:": ladybugData["storageId"],
                "Storage:": ctrl.storage,
                "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
            },
            notifications: {0: {level: "ok", text: "I see you are displaying a report!"}}
        };
        let stubStrategySelect = ladybugData["stubStrategy"];
        ctrl.stubStrategies = $scope.testtoolStubStrategies;
        ctrl.stubStrategySelect = stubStrategySelect;

    }

    ctrl.close_notification = function (key) {
        delete ctrl.reportDetails.notifications[key];
    }

    ctrl.escape_html = function(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    ctrl.highlight_code = function () {
        console.log("Highlightin!!");
        document.querySelectorAll('pre code').forEach((block) => {
            console.log("LOL");
            hljs.highlightBlock(block);
        });
    }

    ctrl.isReportNode = function (node) {
        if (node !== undefined && "ladybug" in node) {
            ctrl.displayingReport = node["ladybug"]["stubStrategy"] === undefined;
        }
        return ctrl.displayingReport;
    }

    ctrl.$onInit = function () {
        if ("select" in ctrl.onSelectRelay) {
            let method = ctrl.onSelectRelay.select;
            ctrl.onSelectRelay.select = function (rootNode, event, node) {
                method(rootNode, event, node);
                ctrl.display_report(rootNode, event, node);
            }
        } else {
            ctrl.onSelectRelay.select = ctrl.display_report;
        }

        ctrl.stubStrategies = $scope.testtoolStubStrategies;
    }
}

angular.module('myApp').component('reportDisplay', {
    templateUrl: 'components/display/display.html',
    controller: ['$scope', '$http', displayController],
    bindings: {
        onSelectRelay: '=',
        applyTransformer: '=',
        storage: '='
    }
});