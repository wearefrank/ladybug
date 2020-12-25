function treeController() {
    let ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.treeData = [];
    ctrl.reports = [];

    ctrl.$onInit = function () {
        console.log("ONINITTT");
        ctrl.onAddRelay.add = ctrl.addTree;
    }

    ctrl.remove_circulars = function (node) {
        if ("parent" in node) {
            delete node["parent"];
        }
        if ("nodes" in node) {
            for (let i = 0; i < node["nodes"].length; i++) {
                ctrl.remove_circulars(node["nodes"][i]);
            }
        }
    };

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        let queryString = "?";
        for (let i = 0; i < ctrl.treeData.length; i++) {
            queryString += "id=" + ctrl.treeData[i]["ladybug"]["storageId"] + "&";
        }
        window.open(ctrl.apiUrl + "/report/download/" + ctrl.storage +
            "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
    }

    ctrl.getRootNode = function () {
        let tree = $('#tree').treeview(true);
        if (tree.getSelected().length === 0)
            return;
        let node = tree.getSelected()[0];
        while (node.parentId !== undefined) {
            node = tree.getNode(node.parentId);
        }
        return node;
    }

    ctrl.closeAll = function () {
        ctrl.treeData = [];
        $('#tree').treeview({data: []});
        ctrl.$apply();
    };

    ctrl.expandAll = function () {
        $('#tree').treeview('expandAll', {levels: 99, silent: true});
    }

    ctrl.collapseAll = function () {
        $('#tree').treeview('collapseAll', {silent: true});
    }

    ctrl.updateTree = function () {
        $('#tree').treeview({
            data: ctrl.treeData,
            onNodeSelected: ctrl.onSelectRelay,
            onNodeUnselected: function (event, node) {
                console.log("UNSELECTED");
                console.log(event);
                console.log(node);
                ctrl.onSelectRelay(null, null);
            }
        });
    }

    /*
     * Adds the given report data to the tree.
     */
    ctrl.addTree = function (data){
        console.log("CALLED ADDTREE!!!!");
        ctrl.reports[data["storageId"]] = data;
        let report = {
            text: data["name"],
            ladybug: data,
            icon: "fa fa-file-code",
            nodes: []
        };
        let checkpoints = data["checkpoints"];
        let nodes = report.nodes;
        let previous_node = null;
        for (let i = 0; i < checkpoints.length; i++) {
            let chkpt = checkpoints[i];
            let node = {text: chkpt["name"], ladybug: chkpt, level: chkpt["level"]};

            if (previous_node === null) {
                nodes.push(node);
                previous_node = node;
                console.log(" -- Added: " + node.text);
                continue;
            }
            let diff = (node.level - 1) - previous_node.level;

            while (diff !== 0) {
                console.log("aim: " + node.level);
                console.log("prev: " + previous_node.level);
                console.log("Prev Text: " + previous_node.text);
                console.log("diff: " + diff);
                if (diff < 0) {
                    previous_node = previous_node.parent;
                } else {
                    if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                        break;
                    }
                    previous_node = previous_node.nodes[previous_node.nodes - 1];
                }
                diff = (node.level - 1) - previous_node.level;
            }
            if (!("nodes" in previous_node) || previous_node["nodes"] === null) {
                previous_node["nodes"] = [];
            }
            previous_node["nodes"].push(node);
            node.parent = previous_node;
            previous_node = node;
            console.log(" -- Added: " + node.ladybug["index"] + " " + node.text);
            console.log(report);
        }
        ctrl.remove_circulars(report);
        ctrl.treeData.push(report);
        console.log(ctrl.treeData);
        // Update tree
        ctrl.updateTree();
    };

    ctrl.close = function () {
        let storageId = ctrl.getRootNode()["ladybug"]["storageId"];
        for (let i = 0; i < ctrl.treeData.length; i++) {
            if (storageId === ctrl.treeData[i]["ladybug"]["storageId"]) {
                ctrl.treeData.splice(i, 1);
                i--;
            }
        }
        ctrl.updateTree();
        ctrl.onSelectRelay(null, null);
    }

    ctrl.onSelectRelay = function(event, node) {
        let rootNode = ctrl.getRootNode();
        ctrl.onSelect({rootNode: rootNode, node: node, event:event});
    }
}

angular.module('myApp').component('reportTree', {
    templateUrl: 'components/tree/tree.html',
    controller: treeController,
    bindings: {
        onSelect: '&',
        onAddRelay: '='
    }
});