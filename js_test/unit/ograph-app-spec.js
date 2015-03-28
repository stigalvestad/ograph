describe('OgraphCtrl', function() {

	var scope, runnerService;
	
	var twoResultsJohan = [{
		resultId: 114, 
		runner: {eventorId: 124, firstName: 'Johan', lastName: 'Moan'}, 
		organisation: {eventorId: 712, name: 'Wing OK'}, 
		raceClass: {eventorId: 814, 
			race: {eventorId: 412, name: 'Delten', raceDate: 123459, classificationId: 1, statusId: 2, url: 'http://www.delten.no'}, 
			name: "H50", nofStarts: 50},
        duration: 1000 * 60 * 42, raceStatus: 'OK', rank: 15, timeBehind: 1000 * 60 * 3
	},
	{
		resultId: 115, 
		runner: {eventorId: 124, firstName: 'Johan', lastName: 'Moan'}, 
		organisation: {eventorId: 712, name: 'Wing OK'}, 
		raceClass: {eventorId: 815, 
			race: {eventorId: 411, name: 'Dilten', raceDate: 123456, classificationId: 1, statusId: 2, url: 'http://www.dilten.no'}, 
			name: "H21", nofStarts: 10, distance: 3.0},
        duration: 1000 * 60 * 15, raceStatus: 'DSQ', rank: 4, timeBehind: 1000 * 60 * 1
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
	var serviceUsage;

	beforeEach(module('ographApp'));

	beforeEach(function() {
		serviceUsage = {
				runnerService: {
					update: [], 
					results: [], 
					races: []
				}
		};
		var mockRunnerService = {};
		module('ographControllers', function($provide) { // or should i load ographApp?
			$provide.value('Runner', mockRunnerService);
		});

		inject(function($q) {
			
			var retrievalLogOk = {statusCode: 200, statusText: 'OK'};
			
			mockRunnerService.update = function(arg1) {
				console.log('update runner');
				serviceUsage.runnerService.update.push(arg1);
				var defer = $q.defer();
				
				defer.resolve(retrievalLogOk);

				return defer.promise;
			};
			
			mockRunnerService.results = function(arg1, arg2) {
				serviceUsage.runnerService.results.push(arg1);
				console.log('get results');
				
				var defer = $q.defer();

				defer.resolve(fourResultsStig);

				return defer.promise;
			};
			
			mockRunnerService.races = function(arg1) {
				serviceUsage.runnerService.races.push(arg1);
				console.log('get races');
				
				var defer = $q.defer();

				defer.resolve([]);

				return defer.promise;
			};
		});
	});

	beforeEach(inject(function($controller, $rootScope, _Runner_) {
		scope = $rootScope.$new();
		runnerService = _Runner_;

		$controller('OgraphCtrl', {
			$scope : scope,
			Runner : runnerService,
			'_' : _
		});

		scope.$digest();
	}));

	it('should have this state at startup', function() {
		expect(scope.resultLoadStatus).toEqual([]);
		expect(scope.resultsPerRunner).toEqual([]);
		expect(scope.selectedRunners).toEqual([]);
		expect(scope.messages).toEqual([]);
		expect(scope.graphDataWithConfig.series).toEqual([]);
		
		expect(scope.graphType).toEqual('rank');

	});

	describe('set graph type', function() {

		it('should update graph type', function() {
			scope.setGraphType('min-after');
			expect(scope.graphType).toEqual('min-after');
		});
	});
	

	describe('when raceFetchStatus changes', function() {

		it('should update resultLoadStatus for all runners', function() {
			scope.resultLoadStatus = [
			                          {eventorId: 111, status: 'FETCHING_RACE_RESULTS', progress: 25}, 
			                          {eventorId: 222, status: 'FETCHING_RACE_RESULTS', progress: 25}];
			expect(scope.racesRunnersHaveParticipatedIn).toEqual([]);
			scope.racesRunnersHaveParticipatedIn = [
			                                        {runnerEventorId: 111, raceEventorIds: [123, 456]}, 
			                                        {runnerEventorId: 222, raceEventorIds: [123, 789]}];
			scope.raceFetchStatus = [
			                         {eventorId: 123, fetchStatus: 'ok'}, 
			                         {eventorId: 789, fetchStatus: 'ok'}];
			scope.$digest();
			
			expect(serviceUsage.runnerService.results).toEqual([{runnerId: 222}]);
			
			expect(scope.resultLoadStatus.length).toEqual(2);
			expect(scope.resultLoadStatus[0]).toEqual({eventorId: 111, status: 'FETCHING_RACE_RESULTS', progress: 50});
			expect(scope.resultLoadStatus[1]).toEqual({eventorId: 222, status: 'FETCHING_RUNNER_RESULTS', progress: 75});
		});
		
		it('should not ask results for runner, if that process is already started', function() {
			scope.resultLoadStatus = [{eventorId: 222, status: 'FETCHING_RUNNER_RESULTS', progress: 75}];
			
			scope.raceFetchStatus = [
			                         {eventorId: 123, fetchStatus: 'ok'}, 
			                         {eventorId: 789, fetchStatus: 'failed'}];
			
			scope.racesRunnersHaveParticipatedIn = [{runnerEventorId: 222, raceEventorIds: [123, 789]}];
			
			scope.$digest();
			
			verifyNoInteractionWithMocks();

			expect(scope.resultLoadStatus.length).toEqual(1);
			expect(scope.resultLoadStatus[0]).toEqual({eventorId: 222, status: 'FETCHING_RUNNER_RESULTS', progress: 75});
		});
	});
		

	describe('get alert level', function() {

		it('defaults to info if no messages', function() {
			expect(scope.getAlertLevel()).toEqual('info');
		});

		it('is danger if at least one danger message', function() {
			scope.messages.push({text: '', level: 'danger'});
			scope.messages.push({text: '', level: 'success'});
			scope.messages.push({text: '', level: 'info'});
			scope.messages.push({text: '', level: 'warning'});
			expect(scope.getAlertLevel()).toEqual('danger');
		});
		
		it('is warning if at least one warning message and no danger', function() {
			scope.messages.push({text: '', level: 'success'});
			scope.messages.push({text: '', level: 'info'});
			scope.messages.push({text: '', level: 'warning'});
			expect(scope.getAlertLevel()).toEqual('warning');
		});

		it('is success if at least one success message and no danger and warning', function() {
			scope.messages.push({text: '', level: 'success'});
			scope.messages.push({text: '', level: 'info'});
			expect(scope.getAlertLevel()).toEqual('success');
		});

		it('is info if at least one info message and no other types', function() {
			scope.messages.push({text: '', level: 'info'});
			expect(scope.getAlertLevel()).toEqual('info');
		});
	});
	
	
	describe('results loaded ok?', function() {
		it('should return false if load status != DONE', function() {
			scope.resultLoadStatus.push({eventorId: 123, status: '?'});
			var loadedOk = scope.resultsLoadedOk(123);
			expect(loadedOk).toEqual(false);
		});

		it('should return true if load status == DONE', function() {
			scope.resultLoadStatus.push({eventorId: 123, status: 'DONE'});
			var loadedOk = scope.resultsLoadedOk(123);
			expect(loadedOk).toEqual(true);
		});
		
		it('should return undefined if load status is missing', function() {
			var loadedOk = scope.resultsLoadedOk(123);
			expect(loadedOk).toEqual(undefined);
		});
	});
	
	function verifyNoInteractionWithMocks(){
		expect(serviceUsage.runnerService.update.length).toEqual(0);
		expect(serviceUsage.runnerService.results.length).toEqual(0);
		expect(serviceUsage.runnerService.races.length).toEqual(0);
	}
	
	describe('when selectedRunners changes', function(){
		it('should clean up data for runner, if runner is removed', function(){
			
			scope.selectedRunners = [{eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, {eventorId: 114, firstName: 'Johan', lastName: 'Moan'}];
			scope.resultsPerRunner = [{eventorId: 123, results: fourResultsStig}, {eventorId: 114, results: twoResultsJohan}];
			scope.resultLoadStatus = [{eventorId: 123, status: 'DONE'}, {eventorId: 114, status: 'DONE'}];
			
			scope.$digest();
			
			scope.selectedRunners = [{eventorId: 114, firstName: 'Johan', lastName: 'Moan'}];

			scope.$digest();
			
			expect(scope.selectedRunners).toEqual([{eventorId: 114, firstName: 'Johan', lastName: 'Moan'}]);
			expect(scope.resultsPerRunner).toEqual([{eventorId: 114, results: twoResultsJohan}]);
			expect(scope.resultLoadStatus).toEqual([{eventorId: 114, status: 'DONE'}]);
			
			verifyNoInteractionWithMocks();
		});
		
		it('should fetch list of races runner has participated in, if a new runner is selected', function(){
			
			scope.selectedRunners = [{eventorId: 114, firstName: 'Johan', lastName: 'Moan'}];
			
			scope.resultsPerRunner = [{eventorId: 114, results: twoResultsJohan}];
			scope.resultLoadStatus = [{eventorId: 114, status: 'DONE'}];
			
			scope.$digest();
			
			scope.selectedRunners = [{eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'}, {eventorId: 114, firstName: 'Johan', lastName: 'Moan'}];

			scope.$digest();
			
			expect(scope.resultLoadStatus).toEqual([{eventorId: 114, status: 'DONE'}, {eventorId: 123, status: 'FETCHING_RACE_LIST', progress: 0}]);
			
			expect(serviceUsage.runnerService.races).toEqual([{runnerId: 123}]);
			expect(serviceUsage.runnerService.results).toEqual([]);
			expect(serviceUsage.runnerService.update).toEqual([]);
		});
	});
	
	describe('when resultLoadStatus changes', function(){
		it('should not update graph when results are not finished loading', function(){
			scope.resultLoadStatus.push({eventorId: 123, status: '?'});
			
			scope.$digest();
			expect(scope.graphDataWithConfig.series).toEqual([]);
		});
		
		it('should update graph data if results have finished loading for at least one new runner', function(){
			// graph type is rank by default
			scope.selectedRunners.push({eventorId: 123, firstName: 'Stig', lastName: 'Alvestad'});
			scope.resultsPerRunner.push({eventorId: 123, results: fourResultsStig});
			scope.resultLoadStatus.push({eventorId: 123, status: 'DONE'});
			
			scope.$digest();
			
			expect(scope.messages.length).toEqual(0);
			
			expect(scope.graphDataWithConfig.series.length).toEqual(1);
			expect(scope.graphDataWithConfig.series[0].name).toEqual('Stig Alvestad');
			expect(scope.graphDataWithConfig.series[0].runnerId).toEqual(123);
			expect(scope.graphDataWithConfig.series[0].data.length).toEqual(3);
			
			expect(scope.graphDataWithConfig.series[0].data[0][0]).toEqual(123456);
			expect(scope.graphDataWithConfig.series[0].data[0][1]).toEqual(4);
			
			expect(scope.graphDataWithConfig.series[0].data[1][0]).toEqual(123457);
			expect(scope.graphDataWithConfig.series[0].data[1][1]).toEqual(3);
			
			expect(scope.graphDataWithConfig.series[0].data[2][0]).toEqual(123459);
			expect(scope.graphDataWithConfig.series[0].data[2][1]).toEqual(12);
			
			scope.graphType = 'duration';
			scope.$digest();
			
			expect(scope.messages.length).toEqual(0);
			
			expect(scope.graphDataWithConfig.series[0].name).toEqual('Stig Alvestad');
			expect(scope.graphDataWithConfig.series[0].data.length).toEqual(3);
			
			expect(scope.graphDataWithConfig.series[0].data[0][0]).toEqual(123456);
			expect(scope.graphDataWithConfig.series[0].data[0][1]).toEqual(15);
			
			expect(scope.graphDataWithConfig.series[0].data[1][0]).toEqual(123457);
			expect(scope.graphDataWithConfig.series[0].data[1][1]).toEqual(92);

			expect(scope.graphDataWithConfig.series[0].data[2][0]).toEqual(123459);
			expect(scope.graphDataWithConfig.series[0].data[2][1]).toEqual(72);
			
			scope.graphType = 'min-km';
			scope.$digest();
			
			expect(scope.messages.length).toEqual(1);
			expect(scope.messages[0]).toEqual({
				text: 'Resultatet fra Delten er utelatt for Stig Alvestad, fordi løypelengden til H21 mangler.', level: 'info', ordinalLevel: 0});
			
			expect(scope.graphDataWithConfig.series.length).toEqual(1);
			expect(scope.graphDataWithConfig.series[0].name).toEqual('Stig Alvestad');
			expect(scope.graphDataWithConfig.series[0].data.length).toEqual(2);
			
			expect(scope.graphDataWithConfig.series[0].data[0][0]).toEqual(123456);
			expect(scope.graphDataWithConfig.series[0].data[0][1]).toEqual(5);
			
			expect(scope.graphDataWithConfig.series[0].data[1][0]).toEqual(123457);
			expect(scope.graphDataWithConfig.series[0].data[1][1]).toEqual(9.2);
			
			scope.graphType = 'after-min';
			scope.$digest();
			
			expect(scope.messages.length).toEqual(0);
			
			expect(scope.graphDataWithConfig.series.length).toEqual(1);
			expect(scope.graphDataWithConfig.series[0].name).toEqual('Stig Alvestad');
			expect(scope.graphDataWithConfig.series[0].data.length).toEqual(3);
			
			expect(scope.graphDataWithConfig.series[0].data[0][0]).toEqual(123456);
			expect(scope.graphDataWithConfig.series[0].data[0][1]).toEqual(1);
			
			expect(scope.graphDataWithConfig.series[0].data[1][0]).toEqual(123457);
			expect(scope.graphDataWithConfig.series[0].data[1][1]).toEqual(4);
			
			expect(scope.graphDataWithConfig.series[0].data[2][0]).toEqual(123459);
			expect(scope.graphDataWithConfig.series[0].data[2][1]).toEqual(5);

			scope.graphType = 'after-min-km';
			scope.$digest();
			
			expect(scope.messages.length).toEqual(1);
			expect(scope.messages[0]).toEqual({
				text: 'Resultatet fra Delten er utelatt for Stig Alvestad, fordi løypelengden til H21 mangler.', level: 'info', ordinalLevel: 0});
			
			expect(scope.graphDataWithConfig.series.length).toEqual(1);
			expect(scope.graphDataWithConfig.series[0].name).toEqual('Stig Alvestad');
			expect(scope.graphDataWithConfig.series[0].data.length).toEqual(2);
			
			expect(scope.graphDataWithConfig.series[0].data[0][0]).toEqual(123456);
			expect(scope.graphDataWithConfig.series[0].data[0][1]).toEqual(1/3);
			
			expect(scope.graphDataWithConfig.series[0].data[1][0]).toEqual(123457);
			expect(scope.graphDataWithConfig.series[0].data[1][1]).toEqual(0.4);
		});
	})

});