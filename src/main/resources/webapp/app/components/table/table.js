function metadataTableController($http) {
    var ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.columns = ['storageId', 'endTime', 'duration', 'name', 'correlationId', 'status', 'nrChpts', 'estMemUsage', 'storageSize'];
    ctrl.storage = "debugStorage";
    ctrl.limit = 5;
    ctrl.filters = {"limit": ctrl.limit, "sort": "", "order": "ascending"};
    ctrl.metadatas = [];
    ctrl.options = {
        "generatorEnabled": true,
        "regexFilter": "^(?!Pipeline WebControl).*",
        "estMemory": 0,
        "reportsInProgress": 0,
        "transformation": "",
        "transformationEnabled": true,
        "openLatest": 10,
        "openInProgress": 1
    };

    /*
    * Sends a GET request to the server and updates the tables (ctrl.metadatas)
    */
    ctrl.updateTable = function () {
        $http.get(ctrl.apiUrl + "/metadata/" + ctrl.storage, {params: ctrl.filters}).then(function (response) {
            let fields = response.data["fields"];
            let values = response.data["values"];
            ctrl.metadatas = [];
            values.forEach(function (element) {
                if (fields.length !== element.length) {
                    return;
                }
                let row = {};
                for (let i = 0; i < fields.length; i++) {
                    row[fields[i]] = element[i];
                }
                ctrl.metadatas.push(row);
            });
        }, function (response) {
            console.error(response);
        });
    };
    ctrl.delete = function() {
        ctrl.onDelete({hero: ctrl.hero});
    };

    ctrl.update = function(prop, value) {
        ctrl.onUpdate({hero: ctrl.hero, prop: prop, value: value});
    };


    ctrl.downloadReports = function (exportReport, exportReportXml) {
        let queryString = "?";
        for (let i = 0; i < ctrl.metadatas.length; i++) {
            queryString += "id=" + ctrl.metadatas[i]["storageId"] + "&";
        }

        window.open(ctrl.apiUrl + "/report/download/" + ctrl.storage +
            "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
    }

    /*
     * Refreshes the content of the table, including getting new columns
     */
    ctrl.refresh = function () {
        $http.get(ctrl.apiUrl + '/metadata').then(function (response) {
            ctrl.columns = response.data;
            ctrl.columns.forEach(function (element) {
                if (ctrl.filters[element] === undefined) {
                    ctrl.filters[element] = "";
                }
            });
            ctrl.updateTable();
        }, function (response) {
            console.error(response);
        });
    };

    ctrl.uploadReport = function() {
        let files = document.getElementById("upload-file").files;
        if (files.length === 0)
            return;
        for (let i = 0; i < files.length; i++) {
            let formdata = new FormData();
            formdata.append("file", files[i]);
            $http.post(ctrl.apiUrl + "/report/upload/", formdata, {headers: {"Content-Type": undefined}})
                .then(function (response) {
                    for (let i = 0; i < response.data.length; i++) {
                        ctrl.onSelectRelay.add(response.data[i]);
                    }
                }, function (response) {
                    console.log(response);
                });
        }
    }


    ctrl.updateFilter = function (key, value) {
        ctrl.filters[key] = value;
        ctrl.updateTable();
    }

    ctrl.updateOrder = function (column) {
        ctrl.filters["sort"] = column;
        if (ctrl.filters["order"] === "descending") {
            ctrl.filters["order"] = "ascending";
        } else {
            ctrl.filters["order"] = "descending";
        }
        ctrl.updateTable();
    }

    ctrl.selectReport = function (metadata) {
        console.log("CLICK");
        console.log(metadata);
        $http.get(ctrl.apiUrl + "/report/" + ctrl.storage + "/" + metadata["storageId"])
            .then(function (response) {
                console.log(response);
                console.log(response.data);
                ctrl.onSelectRelay.add(response.data);
            }, function (response) {
                console.error(response);
            });
    };

    ctrl.openAll = function () {
        ctrl.metadatas.forEach(function (element) {
            ctrl.selectReport(element);
        });
    };

    ctrl.updateOptions = function() {
        $http.get(ctrl.apiUrl + "/testtool")
            .then(function (response) {
                ctrl.options = Object.assign(ctrl.options, response.data);
                ctrl.testtoolStubStrategies = response.data["stubStrategies"];
                ctrl.stubStrategies = ctrl.testtoolStubStrategies;
                console.log("/testtool");
                console.log(response);
                console.log(ctrl.options);
            }, function (response) {
                console.log("/testtool failed");
            });
        $http.get(ctrl.apiUrl + "/testtool/transformation")
            .then(function (response) {
                ctrl.options = Object.assign(ctrl.options, response.data);
                ctrl.options['transformationEnabled'] = ctrl.options['transformation'] !== "";
                console.log("/testtool/transformation");
            });
    }

    ctrl.saveOptions = function() {
        console.log("Saving Options");
        console.log(ctrl.options);
        $http.post(ctrl.apiUrl + "/testtool", {
            "generatorEnabled": ctrl.options['generatorEnabled'],
            "regexFilter": ctrl.options["regexFilter"]});
        $http.post(ctrl.apiUrl + "/testtool/transformation", {"transformation": ctrl.options['transformation']});
    }

    ctrl.openLatestReports = function (number) {
        $http.get(ctrl.apiUrl + "/report/latest/" + ctrl.storage + "/" + number)
            .then(function (response) {
                response.data.forEach(function (report) {
                    ctrl.onSelectRelay.add(report);
                    console.log(report);
                });
            });
    }

    ctrl.$onInit = function () {
        ctrl.updateOptions();
        ctrl.refresh();
        ctrl.onSelectRelay.transformationEnabled = ctrl.options.transformationEnabled;
    }
}

angular.module('myApp').component('metadataTable', {
    templateUrl: 'components/table/table.html',
    controller: metadataTableController,
    bindings: {
        onSelectRelay: '='
    }
});