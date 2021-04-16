'use strict';

function toastController() {
    let ctrl = this;
    ctrl.timeText = "Just now";
    console.log("toast with title " + ctrl.title);

    ctrl.$onInit = function () {
        // TODO: Fix. I believe when onInit is called dom is not ready (in a sense that #toast+componentId is not set properly.
        setTimeout(ctrl.initToast, 100);
    }

    ctrl.initToast = function () {
        let toast = $('#toast' + ctrl.componentId);
        toast.toast({delay: 10000});

        // Add listener to cleanup the resources after close.
        toast.on('hidden.bs.toast', function () {
            console.log("Removing toast ", ctrl.componentId)
            $('#' + ctrl.componentId).remove();
        });

        toast.toast('show');
    }
}

angular.module('myApp').component('toast', {
    templateUrl: 'components/toast/toast.html',
    controller: [toastController],
    bindings: {
        title: '@',
        text: '@',
        componentId: '@'
    }
});