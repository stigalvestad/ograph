describe('OgraphServices', function() {

	var twoResultsJohan = [{
		resultId: 114, 
		runner: {eventorId: 124, firstName: 'Johan', lastName: 'Moan'}, 
		organisation: {eventorId: 712, name: 'Wing OK'}, 
		raceClass: {eventorId: 814, 
			race: {eventorId: 412, name: 'Delten', raceDate: 1361574000000, classificationId: 1, statusId: 2, url: 'http://www.delten.no'},
			name: "H50", nofStarts: 50},
        duration: 1000 * 60 * 42, raceStatus: 'OK', rank: 15, timeBehind: 1000 * 60 * 3
	},
	{
		resultId: 115, 
		runner: {eventorId: 124, firstName: 'Johan', lastName: 'Moan'}, 
		organisation: {eventorId: 712, name: 'Wing OK'}, 
		raceClass: {eventorId: 815, 
			race: {eventorId: 411, name: 'Dilten', raceDate: 123456, classificationId: 1, statusId: 2, url: 'http://www.dilten.no'}, 
			name: "H50", nofStarts: 10, distance: 3.0},
        duration: 1000 * 60 * 15, raceStatus: 'DSQ', rank: 4, timeBehind: 1000 * 60 * 1
	}];

    var oneResultRoar = [{
        resultId: 114,
        runner: {eventorId: 125, firstName: 'Roar', lastName: 'Strand'},
        organisation: {eventorId: 712, name: 'RBK'},
        raceClass: {eventorId: 814,
            race: {eventorId: 412, name: 'Delten', raceDate: 1361574000000, classificationId: 1, statusId: 2, url: 'http://www.delten.no'},
            name: "H45", nofStarts: 55},
        duration: 1000 * 60 * 42, raceStatus: 'OK', rank: 8, timeBehind: 1000 * 60 * 2
    }];


    var fourResultsStig = [{
		resultId: 114, 
		runner: {eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, 
		organisation: {eventorId: 711, name: 'Ganddal IL'}, 
		raceClass: {eventorId: 914, 
			race: {eventorId: 412, name: 'Delten', raceDate: 123459, classificationId: 1, statusId: 2, url: 'http://www.delten.no'}, 
			name: "H21", nofStarts: 50},
        duration: 1000 * 60 * 72, raceStatus: 'OK', rank: 12, timeBehind: 1000 * 60 * 5
	},
	{
		resultId: 111, 
		runner: {eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, 
		organisation: {eventorId: 711, name: 'Ganddal IL'}, 
		raceClass: {eventorId: 911, 
			race: {eventorId: 411, name: 'Dilten', raceDate: 123456, classificationId: 1, statusId: 2, url: 'http://www.dilten.no'}, 
			name: "H21", nofStarts: 10, distance: 3.0},
        duration: 1000 * 60 * 15, raceStatus: 'OK', rank: 4, timeBehind: 1000 * 60 * 1
	}, 
	{
		resultId: 112, 
		runner: {eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, 
		organisation: {eventorId: 711, name: 'Ganddal IL'}, 
		raceClass: {eventorId: 912, 
			race: {eventorId: 412, name: 'Dalten', raceDate: 123457, classificationId: 1, statusId: 2, url: 'http://www.dalten.no'}, 
			name: "H21", nofStarts: 20, distance: 10.0},
        duration: 1000 * 60 * 92, raceStatus: 'OK', rank: 3, timeBehind: 1000 * 60 * 4
	}, 
	{
		resultId: 113, 
		runner: {eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, 
		organisation: {eventorId: 711, name: 'Ganddal IL'}, 
		raceClass: {eventorId: 913, 
			race: {eventorId: 413, name: 'Dulten', raceDate: 123458, classificationId: 1, statusId: 2, url: 'http://www.dulten.no'}, 
			name: "H21E", nofStarts: 30, distance: 4.5},
        duration: 1000 * 60 * 93, raceStatus: 'DSQ'
	}];

    var utilMethods;

	beforeEach(module('ographServices'));

    beforeEach(function() {
        inject(function($injector) {
            utilMethods = $injector.get('UtilMethods');
        });
    });

	describe('> util methods', function(){
        describe('> make tooltip content', function(){
            it('should handle single race', function(){
                var resPerRunner = [{eventorId: 124, results: twoResultsJohan}];
                var selectedPoints = {key: 1361574000000, point: {series: {name: 'Johan Moan', userOptions: {runnerId: 124}}}, x: 1361574000000, y: 15};
                var printYVal = function(yVal){
                    return yVal + '. plass';
                };
                var ttContent = utilMethods.makeTooltipContent(printYVal, resPerRunner, selectedPoints);

                expect(ttContent).toEqual(
                    '<strong>Delten 23/2-13</strong>'
                + '<br/>15. plass, Johan Moan, H50');
            });

            //it('should handle results from two runners in same race', function(){
            //    var resPerRunner = [
            //        {eventorId: 124, results: twoResultsJohan},
            //        {eventorId: 125, results: oneResultRoar}];
            //    var selectedPoints = {points: [
            //        {key: 1361574000000, series: {name: 'Johan Moan', runnerId: 124}, y: 15},
            //        {key: 1361574000000, series: {name: 'Roar Strand', runnerId: 125}, y: 8}
            //    ], x: 1361574000000};
            //    var ttContent = utilMethods.makeTooltipContent('. plass', resPerRunner, selectedPoints);
            //
            //    expect(ttContent).toEqual(
            //        '<strong>Delten 23/2-13</strong>'
            //        + '<br/>8. plass, Roar Strand, H45'
            //        + '<br/>15. plass, Johan Moan, H50'
            //    );
            //});

        });

		
	})

});