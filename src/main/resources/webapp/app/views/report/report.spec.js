'use strict';

describe('myApp.report module', function() {

  beforeEach(module('myApp.report'));

  describe('report controller', function(){

    it('should ....', inject(function($controller) {
      //spec body
      console.log("Injecting reportctrl");
      var reportCtrl = $controller('ReportCtrl');
      expect(reportCtrl).toBeDefined();
    }));
  });
});