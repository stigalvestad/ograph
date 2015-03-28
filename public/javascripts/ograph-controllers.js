var ographControllers = angular.module('ographControllers',
		[ 'ographServices', 'underscore' ]);

ographControllers.controller('OgraphCtrl', [ '$scope', 'Runner', 'Race', 'UtilMethods', '_', 'numeral',
		function($scope, Runner, Race, UtilMethods, _, numeral) {

            var POLL_INTERVAL = 500;
	
			/**
			 * Progress
			 * get list of races from eventor: 1 unit
			 * get x races from eventor: x units
			 * get results locally: 1 unit
			 * total: 1 + x + 1 units
			 */
			$scope.resultLoadStatus = [];// [{eventorId: 123, status: .., progress: 0-100]
			$scope.resultsPerRunner = [];// [{eventorId: 123, results: [...]}, ...]
			$scope.selectedRunners = [];
			$scope.messages = []; //[{text: 'Bla bla.', level: 'success/info/warning/danger', numLevel: 1/2/3/4}, ...]
			$scope.graphType = 'rank';
			
			$scope.showFilter = false;
			$scope.showAllMessages = false;
			
			$scope.racesRunnersHaveParticipatedIn = []; // [{runnerEventorId: 123, raceEventorIds: [234, 456, ..., 432]}]
			
			$scope.graphDataWithConfig = {
					series: [], //[{name: runnerName, data: [[raceDate, value]]}, ...] 
					title: ''
			};
			
			$scope.filterOptions = {
					raceClassOptions: [], 
					raceTypeOptions: [], 
					racePeriodOptions: []
			};
			$scope.resultFilter = {
					raceClassFilter: [], 
					raceTypeFilter: [], 
					racePeriodFilter: []
			};
			
			var racesInfo = [];
			
			var msgLevelRanks = [
			                     {level: 'info', rank: 0}, 
			                     {level: 'success', rank: 1}, 
			                     {level: 'warning', rank: 2}, 
			                     {level: 'danger', rank: 3}];

            /**
             * Blodslitet has classificationId = 6, why? Bug in eventor?
             */
			var getRaceTypeLabel = function(classificationId){
				if (classificationId == 1) return 'Mesterskap';
				if (classificationId == 2) return 'Nasjonalt løp';
				if (classificationId == 3) return 'Kretsløp';
				if (classificationId == 4) return 'Nærløp';
				if (classificationId == 5) return 'Klubbløp';
				else return '???';
			};

			var getMessageLevelOrdinal = function(level){
				var lr = _.find(msgLevelRanks, function(levelRank){
					return levelRank.level == level;
				});
				return lr.rank;
			};
			
			var resetMessages = function(){
				$scope.messages = [];
			};
			
			var getEventorId = function(runner){return runner.eventorId;};
			var getRecordForRunner = function(eventorId, struct){
				return _.find(struct, function(resultForRunner){
					return getEventorId(resultForRunner) == eventorId;
				});
			};
			
			$scope.getLoadProgressForRunner = function(eventorId){
				var runnerProgress = getStatusForRunner(eventorId).progress;
				if (runnerProgress == 100){
					return '';
				}
				else return ' ' + Math.round(runnerProgress) + '%';
			};
			
			var getStatusForRunner = function(eventorId){
				return getRecordForRunner(eventorId, $scope.resultLoadStatus);
			};

			var getResultsForRunner = function(eventorId){
				var raceResults = Runner.results({runnerId: eventorId}, function(){
					$scope.resultsPerRunner.push({eventorId: eventorId, results: raceResults});
					var runnerLoadStatus = getStatusForRunner(eventorId);
					runnerLoadStatus.status = 'DONE';
					runnerLoadStatus.progress = 100;
				});
			};
			
			var getNameOfRunner = function(eventorId){
				var runnerInfo = getRecordForRunner(eventorId, $scope.selectedRunners);
				return getFullName(runnerInfo);
			};
			
			var getFullName = function(runner){
				return runner.firstName + ' ' + runner.lastName;				
			};

			// ----------- ResultPropertyService Start ------------
			var getTimeBehind = function(result){
				if (result.timeBehind) return result.timeBehind / (1000 * 60);
				else return null;
			};
			
			var getRank = function(result){
				if (result.rank) return result.rank;
				else return null;
			};
			
			var getDuration = function(result){
				if (result.duration) return result.duration / (1000 * 60);
				else return null;
			};
			
			var addMsgWhenDistanceMissing = function(result){
				if (result.duration && !result.raceClass.distance)
					$scope.messages.push({text: 'Resultatet fra '+result.raceClass.race.name+' er utelatt for '+getFullName(result.runner)
						+', fordi løypelengden til '+result.raceClass.name+' mangler.', level: 'info', ordinalLevel: getMessageLevelOrdinal('info')});
			};
			
			var addMsgWhenNofStartsMissing = function(result){
				if (result.rank && !result.raceClass.nofStarts)
					$scope.messages.push({text: 'Resultatet fra '+result.raceClass.race.name+' er utelatt for '+getFullName(result.runner)
						+', fordi antall startende i '+result.raceClass.name+' mangler.', level: 'info', ordinalLevel: getMessageLevelOrdinal('info')});
			};

			var getRelativeRank = function(result){
				if (result.rank && result.raceClass.nofStarts) 
					return (result.raceClass.nofStarts - result.rank + 1) / result.raceClass.nofStarts * 100;
				addMsgWhenNofStartsMissing(result);
				return null;
			};
			
			var getMinPerKm = function(result){
				if (result.duration && result.raceClass.distance) 
					return (result.duration / (1000 * 60)) / result.raceClass.distance;
				addMsgWhenDistanceMissing(result);
				return null;
			};
			
			var getMinPerKmAfterWinner = function(result){
				if (result.timeBehind && result.raceClass.distance) 
					return (result.timeBehind / (1000 * 60)) / result.raceClass.distance;
				addMsgWhenDistanceMissing(result);
				return null;
			};
			
//			----------- ResultPropertyService End ------------
			
			var pickProperty = function(result){
				if ($scope.graphType == 'rank') return getRank(result);
				if ($scope.graphType == 'duration') return getDuration(result);
				if ($scope.graphType == 'min-km') return getMinPerKm(result);
				if ($scope.graphType == 'after-min-km') return getMinPerKmAfterWinner(result);
				if ($scope.graphType == 'after-min') return getTimeBehind(result);
				if ($scope.graphType == 'relative-rank') return getRelativeRank(result);
				return result.rank; // default
			};

            var printPropertyFnGenerator = function(graphType){
                var maxDecimals = function(val){
                    return numeral(val).format('0.00');
                };
                if (graphType == 'rank') return function(yVal){
                    return yVal + '. plass';
                };
                if (graphType == 'duration') return function(yVal){
                    return maxDecimals(yVal) + ' min';
                };
                if (graphType == 'min-km') return function(yVal){
                    return maxDecimals(yVal) + ' min/km';
                };
                if (graphType == 'after-min-km') return function(yVal){
                    return maxDecimals(yVal) + ' min/km etter';
                };
                if (graphType == 'after-min') return function(yVal){
                    return maxDecimals(yVal) + ' min etter';
                };
                if (graphType == 'relative-rank') return function(yVal){
                    return 'relativ plassering: ' + maxDecimals(yVal);
                };
                return function(yVal){
                    return yVal + ' ';
                };
            };
			
			var applyRaceTypeFilter = function(result){
				if ($scope.resultFilter.raceTypeFilter.length == 0) return true;
                return _.filter($scope.resultFilter.raceTypeFilter, function(rtFilter){
					return rtFilter.id == result.raceClass.race.classificationId;
				}).length > 0;
			};
			
			var applyRaceClassFilter = function(result){
				if ($scope.resultFilter.raceClassFilter.length == 0) return true;
                return _.filter($scope.resultFilter.raceClassFilter, function(className){
					return className == result.raceClass.name;
				}).length > 0;
			};
			
			var applyUserFilter = function(result){
				//TODO filter on period
				return applyRaceTypeFilter(result) && applyRaceClassFilter(result);
			};
			
			var updateFilterOptions = function(){
				
				var raceClassOptions = _.chain($scope.resultsPerRunner).reduce(function(memo, runnerResults){
					_.map(runnerResults.results, function(result){
						memo.push(result.raceClass.name);
						return result;
					});
					return memo;
				}, []).filter().uniq().sortBy().value();
				
				var raceTypeOptions = _.chain($scope.resultsPerRunner).reduce(function(memo, runnerResults){
					_.map(runnerResults.results, function(result){
						memo.push({id: result.raceClass.race.classificationId, label: getRaceTypeLabel(result.raceClass.race.classificationId)});
						return result;
					});
					return memo;
				}, []).uniq(function(rt){
					return rt.id;
				}).sortBy(function(rt){
					return rt.id;
				}).value();
				
				//TODO combine with existing options
				
				//TODO update racePeriod
				
				$scope.filterOptions.raceClassOptions = raceClassOptions;
				$scope.filterOptions.raceTypeOptions = raceTypeOptions;
			};
			
			$scope.getTopMessages = function(n){
				return _.first($scope.messages, n);
			};
			
			$scope.showMessages = function(show){
				$scope.showAllMessages = show;	
			};
			
			$scope.showFilters = function(show){
				$scope.showFilter = show;
			};
			
			$scope.addRaceTypeFilter = function(){
				if ($scope.selectedRaceType){
					$scope.resultFilter.raceTypeFilter.push($scope.selectedRaceType);

					// remove from options
					_.remove($scope.filterOptions.raceTypeOptions, function(rt){
						return rt.id == $scope.selectedRaceType.id;
					});
				}
			};

			$scope.addRacePeriodFilter = function(){
				//TODO				
			};
			
			$scope.removeRacePeriodFilter = function(){
				//TODO
			};
			
			$scope.removeRaceClassFilter = function(raceClass){
				_.remove($scope.resultFilter.raceClassFilter, function(raceClassName){
					return raceClass == raceClassName;
				});
				
				$scope.filterOptions.raceClassOptions.push(raceClass);
				$scope.filterOptions.raceClassOptions = _.chain($scope.filterOptions.raceClassOptions).uniq().sortBy().value();
			};

			$scope.removeRaceTypeFilter = function(raceType){
				_.remove($scope.resultFilter.raceTypeFilter, function(rt){
					return raceType.id == rt.id;
				});
				
				$scope.filterOptions.raceTypeOptions.push(raceType);
				$scope.filterOptions.raceTypeOptions = _.chain($scope.filterOptions.raceTypeOptions).uniq(function(rt){
					return rt.id;
				}).sortBy(function(rt){
					return rt.id;
				}).value();
			};

			$scope.addRaceClassFilter = function(){
				if ($scope.selectedRaceClass){
					$scope.resultFilter.raceClassFilter.push($scope.selectedRaceClass);

					// remove from options
					_.remove($scope.filterOptions.raceClassOptions, function(raceClassName){
						return raceClassName == $scope.selectedRaceClass;
					});
				}
			};

			var updateGraph = function(){
				
				var graphData = _.map($scope.resultsPerRunner, function(runnerResults){
					var dataPoints = _.chain(runnerResults.results)
					.filter(function(result){
						return result.raceStatus == 'OK';
					}).filter(applyUserFilter)
					.map(function(result){
						return [result.raceClass.race.raceDate, pickProperty(result)];
					}).filter(function(point){
						return point[1] !== null;
					}).sortBy(function(point){
						return point[0];
					})
					.value();
					var runnerName = getNameOfRunner(runnerResults.eventorId);
					if (dataPoints.length < 2){
						$scope.messages.push({text: 'For lite data: ' + runnerName, level: 'info', ordinalLevel: getMessageLevelOrdinal('info')});
						dataPoints = [];
					}
					
					return {name: runnerName, data: dataPoints, runnerId: runnerResults.eventorId};
				});
				
				var getYMin = function(){
					if ($scope.graphType == 'rank') return 1;
					else return 0;
				};

				var getYMax = function(){
					if ($scope.graphType == 'relative-rank') return 100;
					else return null;
				};
				
				var graphConfig = {
					title: $scope.getGraphTypeLabel($scope.graphType), 
					tooltip: {
                        useHTML: true,
						headerFormat: '<b>{series.name}</b><br>',
                        formatter: function(){
                            var printYVal = printPropertyFnGenerator($scope.graphType);
                            return UtilMethods.makeTooltipContent(printYVal, $scope.resultsPerRunner, this);
                        }
					},
					yAxis: {
						min: getYMin(),
                        max: getYMax()
					}
				};

                debugger;
				$scope.graphDataWithConfig = {
					series: graphData, 
					config: graphConfig
				};
			};
			
			$scope.getAlertLevel = function(){
				var maxRank = _.chain($scope.messages).map(function(msg){
					return _.chain(msgLevelRanks).filter(function(msgLevel){
						return msgLevel.level == msg.level;
					}).map(function(msgLevel){
						return msgLevel.rank;
					}).value();
				}).max().value();
				var msgLevel = _.find(msgLevelRanks, function(msgLevel){
					return msgLevel.rank == maxRank;
				});
				return (msgLevel ? msgLevel.level : 'info');
			};
			
			$scope.$watch('resultFilter', function(newFilter){
				if (! newFilter) return;
				resetMessages();
				updateGraph();
			}, true);
			
			$scope.$watch('graphType', function(newType){
				if (! newType) return;
				resetMessages();
				updateGraph();
			});
			
			$scope.resultsLoadedOk = function(eventorId){
				var status = getStatusForRunner(eventorId);
				return status && status.status == 'DONE';
			};
			
			$scope.setGraphType = function(graphType){
				$scope.graphType = graphType;
			};
			
			$scope.getGraphTypeLabel = function(graphType){
				if (graphType == 'after-min-km') return 'Tid etter vinner (min/km)';
				if (graphType == 'min-km') return 'min/km';
				if (graphType == 'after-min') return 'Tid etter vinner (min)';
				if (graphType == 'rank') return 'Plassering';
				if (graphType == 'duration') return 'Tid (min)';
				if (graphType == 'relative-rank') return 'Plassering, relativ (100% = Nr. 1)';
				return '???';
			};
			
			$scope.raceFetchStatus = []; //[{eventorId: 123, runnerEventorId: 456, fetchStatus: ? | 200 | 400, msg: OK | error desc}]

            var getManyRaceResults = function(raceEventorIds){
                var missing = _.filter(raceEventorIds, function(raceEventorId){
                    return ! _.find($scope.raceFetchStatus, function(s){
                        return s.eventorId == raceEventorId;
                    });
                });
                if (missing.length > 0) {
                    _.map(missing, function(raceEventorId){
                        return $scope.raceFetchStatus.push({eventorId: raceEventorId, fetchStatus: null});
                    });

                    var jobId = UtilMethods.guid();
                    Race.update({raceId: missing.join('-'), jobId: jobId}, onRaceSuccess, onRaceFailure);

                    _.delay(function(){
                        Race.jobStatus({jobId: jobId}, onJobStatusSuccessGenerator(jobId), onJobStatusFailure);
                    }, POLL_INTERVAL);
                }
            };

            var onJobStatusSuccessGenerator = function(jobId){
                var onJobStatusSuccess = function(raceResultsFetchResult){
                    onRaceSuccess(raceResultsFetchResult, true);
                    var total = raceResultsFetchResult.length;
                    var completed = _.filter(raceResultsFetchResult, function(rrStatus){
                        return rrStatus.status != 'PENDING';
                    }).length;

                    if (total > completed){
                        _.delay(function(){
                            Race.jobStatus({jobId: jobId}, onJobStatusSuccessGenerator(jobId), onJobStatusFailure);
                        }, POLL_INTERVAL);
                    }
                };
                return onJobStatusSuccess;
            };

            var onJobStatusFailure = function(){
                //do nothing. It will eventually finish the job anyway
            };

            var onRaceSuccess = function(raceResultsFetchResult, ignoreMessages){

                _.forEach(raceResultsFetchResult, function(raceResFetchStatus){
                    _.chain($scope.raceFetchStatus)
                        .filter(function(raceStatus){
                            return raceStatus.eventorId == raceResFetchStatus.eventorItem.id;
                        }).forEach(function(raceStatus){
                            raceStatus.fetchStatus = raceResFetchStatus.status;
                            raceStatus.msg = raceResFetchStatus.msg;
                            if (raceStatus.fetchStatus != "OK" && ! ignoreMessages){
                                $scope.messages.push({text: 'Kunne ikke hente resultater fra løpet '+raceStatus.eventorId
                                + ' fordi: ' + raceStatus.msg,
                                    level: 'warning', ordinalLevel: getMessageLevelOrdinal('warning')});
                            }
                        });
                });
            };

            var onRaceFailure = function(errorResponse){
                if (errorResponse.status == 424){
                    $scope.messages.push({text: 'Det ser ut som Eventor er utilgjengelig for øyeblikket, kan derfor ikke hente nye resultater.',
                        level: 'warning', ordinalLevel: getMessageLevelOrdinal('warning')});
                }
                else if (errorResponse.status == 408){
                    $scope.messages.push({text: 'Det går uvanlig tregt å hente resultater, kan du prøve på nytt senere?',
                        level: 'danger', ordinalLevel: getMessageLevelOrdinal('danger')});
                }
                else {
                    $scope.messages.push({text: 'Noe har gått feil, beklager.',
                        level: 'danger', ordinalLevel: getMessageLevelOrdinal('danger')});
                }
                // mark all races not fetched successfully as unavailable
                _.chain($scope.raceFetchStatus)
                    .filter(function(raceStatus){
                        return raceStatus.fetchStatus == null;
                    }).forEach(function(raceStatus){
                        raceStatus.fetchStatus = 'Currently not available'
                    });
            };
			
			var getRaces = function(eventorId){
//				[
//				    {
//				        "eventorId": 257,
//				        "name": "Vestlands mesterskap",
//				        "raceDate": 1314453600000,
//				        "classificationId": 3,
//				        "statusId": 5,
//				        "url": "http://www.hilorientering.no"
//				    },
//				    ...
//				    ]
				var races = Runner.races({runnerId: eventorId}, function(){
					
					_.chain(races).filter(function(race){
						return _.find(racesInfo, function(r){
							return r.eventorId == race.eventorId
						}) == undefined;
					}).forEach(function(race){
						racesInfo.push(race);
					});
					
					var raceIds = _.map(races, function(race){return race.eventorId});
					$scope.racesRunnersHaveParticipatedIn.push({runnerEventorId: eventorId, raceEventorIds: raceIds});
					
					//update status for runner
					var nofRacesToFetch = raceIds.length;
					var racesWhichAreFetched = getFetchedRaceIds();
					var nofMissingRaces = _.difference(raceIds, racesWhichAreFetched).length;

					var runnerLoadStatus = getStatusForRunner(eventorId);
					if (nofMissingRaces == 0){
						runnerLoadStatus.status = 'FETCHING_RACE_RESULTS_DONE';	
					}
					else {
						runnerLoadStatus.status = 'FETCHING_RACE_RESULTS';	
					}
					runnerLoadStatus.progress = getProgress(nofRacesToFetch, nofMissingRaces);

					getManyRaceResults(raceIds);
				}, function(errorResponse){
                    var runnerLoadStatus = getStatusForRunner(eventorId);
					runnerLoadStatus.status = 'FAILED';
					runnerLoadStatus.progress = 100;
                    var msg = 'Kunne ikke hente løpene som '+getNameOfRunner(eventorId)
                        + ' har deltatt i, pga ' + errorResponse.statusText + ' (' + errorResponse.status + ')';
                    if (errorResponse.status == 424){
                        msg = 'Det ser ut som Eventor er utilgjengelig for øyeblikket, kan derfor ikke hente nye resultater.';
                    }
                    $scope.messages.push({text: msg,
                        level: 'warning', ordinalLevel: getMessageLevelOrdinal('warning')});
				});
			};
			
			var getProgress = function(nofRacesToFetch, nofMissingRaces){
                return (1 + nofRacesToFetch - nofMissingRaces) / (2 + nofRacesToFetch) * 100;
			};
			
			var getFetchedRaceIds = function(){
				return _.chain($scope.raceFetchStatus).filter(function(raceStatus){
					return raceStatus.fetchStatus != null && raceStatus.fetchStatus != 'PENDING';
				}).map(function(raceStatus){
					return raceStatus.eventorId;
				}).value();
			};
			
			$scope.$watch('resultLoadStatus', function(newLS, oldLS) {

                // triggers fetching of results, if all races have been fetched
				_.chain(newLS).filter(function(runnerStatus){
					return runnerStatus.status == 'FETCHING_RACE_RESULTS_DONE';
				}).forEach(function(runnerStatus){
					runnerStatus.status = 'FETCHING_RUNNER_RESULTS';
					getResultsForRunner(runnerStatus.eventorId);
				});

                //update graph if new runner is 'done' or if a previous 'done'-runner has been removed
                var addedNow = _.chain(newLS).filter(function(runnerStatus){
                    return runnerStatus.status == 'DONE';
                }).filter(function(doneRunner){
                    var prevNotDone = _.find(oldLS, function(oldRunnerStatus){
                        return oldRunnerStatus.status != 'DONE' && doneRunner.eventorId == oldRunnerStatus.eventorId;
                    });
                    return prevNotDone;
                }).value();

                var removedNow = _.chain(oldLS).filter(function(oldRunnerStatus){
                    return oldRunnerStatus.status == 'DONE';
                }).filter(function(oldDoneRunner){
                    var stillPresent = _.find(newLS, function(newRunnerStatus){
                        return oldDoneRunner.eventorId == newRunnerStatus.eventorId;
                    });
                    return ! stillPresent;
                }).value();

				if (addedNow.length > 0 || removedNow.length > 0) {
					updateGraph();
					updateFilterOptions();
				}
			}, true);
			
			$scope.$watch('raceFetchStatus', function(){
				var racesWhichAreFetched = getFetchedRaceIds();
				
				_.map($scope.racesRunnersHaveParticipatedIn, function(racesForRunner){
					var diff = _.difference(racesForRunner.raceEventorIds, racesWhichAreFetched);
					var nofRacesToFetch = racesForRunner.raceEventorIds.length;
					var progress = getProgress(nofRacesToFetch, diff.length);
					
					var runnerLoadStatus = getStatusForRunner(racesForRunner.runnerEventorId);
					if (diff.length == 0){
						if (runnerLoadStatus.status == 'FETCHING_RACE_RESULTS'){
							runnerLoadStatus.status = 'FETCHING_RACE_RESULTS_DONE';
							runnerLoadStatus.progress = progress;
						}
					}
					else {
						runnerLoadStatus.status = 'FETCHING_RACE_RESULTS';
						runnerLoadStatus.progress = progress;
					}
				});
				
			}, true);

			$scope.$watch('selectedRunners', function(newValue, oldValue) {
				resetMessages();
				
				var idsOfOldSelection = _.map(oldValue, getEventorId);
				var idsOfNewSelection = _.map(newValue, getEventorId);
				var newlyAdded = _.difference(idsOfNewSelection, idsOfOldSelection);
				
				_.chain(newlyAdded)
				.reject(getStatusForRunner)
				.map(function(eventorId){
					$scope.resultLoadStatus.push({eventorId: eventorId, status: 'FETCHING_RACE_LIST', progress: 0});
					getRaces(eventorId);
				}).value();
				
				var newlyRemoved = _.difference(idsOfOldSelection, idsOfNewSelection);
				_.map(newlyRemoved, function(eventorId){
					_.remove($scope.resultsPerRunner, function(r){
						return r.eventorId == eventorId;
					});
					_.remove($scope.resultLoadStatus, function(r){
						return r.eventorId == eventorId;
					});
					_.remove($scope.racesRunnersHaveParticipatedIn, function(r){
						return r.runnerEventorId == eventorId;
					});
				})
				
			}, true);

		} ]);
