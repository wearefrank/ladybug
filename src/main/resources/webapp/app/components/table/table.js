function metadataTableController($http) {
    var ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.columns = ['storageId', 'endTime', 'duration', 'name', 'correlationId', 'status', 'nrChpts', 'estMemUsage', 'storageSize'];
    ctrl.storage = "debugStorage";
    ctrl.limit = 5;
    ctrl.filters = {"limit": ctrl.limit};
    ctrl.metadatas = [];

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


    ctrl.downloadReports = function (source, exportReport, exportReportXml) {
        let queryString = "?";
        if (source === "table") {
            for (let i = 0; i < ctrl.metadatas.length; i++) {
                queryString += "id=" + ctrl.metadatas[i]["storageId"] + "&";
            }
        } else if (source === "tree") {
            for (let i = 0; i < ctrl.treeData.length; i++) {
                queryString += "id=" + ctrl.treeData[i]["ladybug"]["storageId"] + "&";
            }
        } else {
            queryString += "id=" + ctrl.getRootNode()["ladybug"]["storageId"] + "&";
        }

        window.open(ctrl.apiUrl + "/report/download/" + ctrl.storage +
            "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
        // $http.get(ctrl.apiUrl + "/report/download/" + ctrl.storage + "/true/true?id=" + storageId, {data: [storageId]})
        //     .then(function (response) {
        //         console.log(response);
        //     }, function (response) {
        //         console.error(response);
        //     });
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
            $http.post(ctrl.apiUrl + "/report/upload/" + ctrl.storage, formdata, {headers: {"Content-Type": undefined}});
        }
    }


    ctrl.updateFilter = function (key, value) {
        ctrl.filters[key] = value;
        ctrl.updateTable();
    }

    ctrl.selectReport = function (metadata) {
        console.log("CLICK");
        console.log(metadata);
        $http.get(ctrl.apiUrl + "/report/" + ctrl.storage + "/" + metadata["storageId"])
            .then(function (response) {
                console.log(response);
                console.log(response.data);
                ctrl.onSelect({data: response.data});
            }, function (response) {
                console.error(response);
            });
    };

    ctrl.openAll = function () {
        ctrl.metadatas.forEach(function (element) {
            ctrl.selectReport(element);
        });
    };

    ctrl.refresh();
}

angular.module('myApp').component('metadataTable', {
    templateUrl: 'components/table/table.html',
    controller: metadataTableController,
    bindings: {
        onSelect: '&'
    }
});