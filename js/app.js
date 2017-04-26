(function() {
	var app = angular.module('FabFlix', ['ui.router']);
  
  // http://stackoverflow.com/questions/19254029/angularjs-http-post-does-not-send-data/35699599
	angular.module('FabFlix')
		.config(function ($httpProvider, $httpParamSerializerJQLikeProvider){
			$httpProvider.defaults.transformRequest.unshift($httpParamSerializerJQLikeProvider.$get());
			$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded; charset=utf-8';
	});
  
	app.run(function($rootScope, $location, $state, LoginService) {
		$rootScope.$on('$stateChangeStart', 
			function(event, toState, toParams, fromState, fromParams){ 
				console.log('Changed state to: ' + toState);
			}
		);
		
		$rootScope.offset = 0;	// by default, we look at the first search reuslts from the home controller
		$rootScope.limit = 10;	// by default, we limit the search results to 10 per page from the home controller
			
		if(!LoginService.isAuthenticated()) {
			$state.transitionTo('login');
		}
	});
  
	app.config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
		$urlRouterProvider.otherwise('/home');

		$stateProvider
			.state('login', {
				url : '/login',
				templateUrl : 'html/login.html',
				controller : 'LoginController'
			})
			.state('home', {
				url : '/home',
				templateUrl : 'html/home.html',
				controller : 'HomeController',
				params: {userData: null}
			})
			.state('results', {
				url : '/results?query_type={}starts_with={}order_type={}order_by={}offset={}limit={}',
				templateUrl : 'html/results.html',
				controller : 'ResultsController',
				params: {
					userData: null,
					query_display: null, 
					query_type: null,
					starts_with: null,
					order_by: null,
					order_type: null,
					offset: null,
					limit: null
				}
			})
		}]
	);

	app.controller('LoginController', function($scope, $rootScope, $stateParams, $state, $http, LoginService) {
		$rootScope.title = "FabFlix Login";
		$scope.loggingIn = false;

		$scope.formSubmit = function() {
			$scope.loggingIn = true;
  
			LoginService.login($http, $scope.email, $scope.password)
				.then(function(data) {
					if (data && data.error == "null" && data.message == "success" && data.user != "null") {
						$scope.error = '';
						$scope.email = '';
						$scope.password = '';
						$rootScope.user = data.user;
					$state.go('home', {userData: data.user});
    
					} else {
						if (!data)
							$scope.error = "Error connecting to the server. Please try again.";
						else if (data.error == "null")
							$scope.error = data.message;
						else
							$scope.error = data.error + ": " + data.message;
					}
					$scope.loggingIn = false;
				}
			);
		};
	});
  
	app.controller('HomeController', function($scope, $rootScope, $stateParams, $state, $http, SearchService) {
		$rootScope.title = "Home";
		$scope.userData = $stateParams.userData;
		$scope.genreList = ["Action","Adventure","Animation", "Biography", "Classic", "Comedy", "Coming-of-Age-Drama",
		                    "Crime", "Documentary", "Drama", "Family", "Fantasy", "Foreign", "Gangster", "History",
		                    "Horror", "Indie", "James Bond", "Music", "Musical", "Musical/Performing Arts", "Mystery", 
		                    "Roman", "Romance", "Sci-Fi", "Sport", "Spy", "Suspense", "Thriller", "War"];
		$scope.charList = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		$scope.browseByMovieTitle = function(event) {
			if (!event || !event.target || !event.target.text)
				return;
			
			var char = event.target.text;
			
			$state.go('results', {
				userData: $scope.userData,
				query_type: "BROWSE_BY_MOVIE_TITLE",
				starts_with: char,
				order_by: "title",	// by default
				order_type: "asc",	// by default
				offset: $rootScope.offset,
				limit: $rootScope.limit,
				query_display: "Movies starting with '" + char + "'"	// IMPORTANT: For displaying proper response when no results found!
			});
		}
	});
	
	app.controller('ResultsController', function($scope, $rootScope, $stateParams, $state, $http, SearchService) {
		$rootScope.title = "Results";
		
		$scope.userData = $stateParams.userData;
		$scope.query_type = $stateParams.query_type;
		$scope.query_display = $stateParams.query_display;
		$scope.order_by = $stateParams.order_by;
		$scope.order_type = $stateParams.order_type;
		$scope.offset = Number($stateParams.offset);
		$scope.limit = $stateParams.limit;
		$scope.starts_with = $stateParams.starts_with;
		
		$scope.loading = true;
		$scope.TITLE_COLUMN = "Title";
		$scope.YEAR_COLUMN = "Year";
		
		$scope.LIMIT_10 = 10;
		$scope.LIMIT_25 = 25;
		$scope.LIMIT_50 = 50;
		$scope.LIMIT_100 = 100;
		
		$scope.display_order_type = $scope.order_type == "asc" ? "Ascending" : "Descending";
		$scope.ASCENDING_OPTION = {display: "Ascending", value: "asc"};
		$scope.DESCENDING_OPTION = {display: "Descending", value: "desc"};
		
		if ($stateParams.query_type == "BROWSE_BY_MOVIE_TITLE") {
			SearchService.browseByMovieTitle($http, 
					$stateParams.starts_with, 
					$stateParams.order_by, $stateParams.order_type,
					$stateParams.offset, $stateParams.limit)
				.then(function(data) {
					if (data) {
						console.log(data);
						$scope.data = data.movie_data;
						$scope.number_of_results = Number(data.number_of_results);
						
						$scope.previousButtonStyle = {cursor: 'pointer'};
						$scope.nextButtonStyle = {cursor: 'pointer'};
						
						$scope.previousButtonDisabled = $scope.offset <= 0;
						$scope.nextButtonDisabled = $scope.offset + $scope.data.length >= $scope.number_of_results;
						
						if ($scope.previousButtonDisabled) {
							angular.element( document.querySelector( '#previousButton' ) )
								.addClass('disabled');
							$scope.previousButtonStyle.cursor = 'default';
						}
						if ($scope.nextButtonDisabled) {
							angular.element( document.querySelector( '#nextButton' ) )
								.addClass('disabled');
							$scope.nextButtonStyle.cursor = 'default';
						}
						
						
						var currentPage = Math.floor($scope.number_of_results/$scope.limit) + 1;
						$scope.paginations = []
					} else {
						$scope.data = null;
						$scope.number_of_results = 0;
					}
					$scope.loading = false;
				}
			);
		}
		
		$scope.adjustOrderBySorting = function(new_order_by_sorting) {
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: new_order_by_sorting,
				order_type: $scope.order_type,
				offset: $scope.offset,
				limit: $scope.limit,
				query_display: $scope.query_display
			});
		}
		
		$scope.adjustOrderBySortingType = function(new_sort_type) {
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: $scope.order_by,
				order_type: new_sort_type,
				offset: $scope.offset,
				limit: $scope.limit,
				query_display: $scope.query_display
			});
		}
		
		$scope.adjustSearchLimit = function(new_limit) {
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: $scope.order_by,
				order_type: $scope.order_type,
				offset: $scope.offset,
				limit: new_limit,
				query_display: $scope.query_display
			});
		}
		
		$scope.getNextResults = function() {
			if ($scope.nextButtonDisabled)
				return;
			
			var newOffset = Number($scope.offset) + Number($scope.limit);
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: $scope.order_by,
				order_type: $scope.order_type,
				offset: newOffset,
				limit: $scope.limit,
				query_display: $scope.query_display
			});
		}
		
		$scope.getPreviousResults = function() {
			if ($scope.previousButtonDisabled)
				return;
			
			var newOffset = Number($scope.offset) - Number($scope.limit);
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: $scope.order_by,
				order_type: $scope.order_type,
				offset: newOffset,
				limit: $scope.limit,
				query_display: $scope.query_display
			});
		}
	});
  
	app.factory('LoginService', function() {
		var data = null;
  
		return {
			login : function($http, email, password) {
				// HTTP POST runs asnychronously
				return $http({
					method: 'POST',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixLoginServlet',
					data: { email: email, password: password }
				}).then(function successCallback(response) {
					data = response.data;
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			},
			isAuthenticated : function() {
				return data !== null;
			}
		};
  });
  
	app.factory('SearchService', function() {
		return {
			browseByMovieTitle : function($http, char, order_by, order_type, offset, limit) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixSearchServlet',	
					params: { 
						query_type: "BROWSE_BY_MOVIE_TITLE",
						starts_with: char,
						order_by: order_by,
						order_type: order_type,
						offset: offset,
						limit: limit
					}
				// IMPORTANT: query_type parameter determines the type of GET operation to use!
				}).then(function successCallback(response) {
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			}
		};
	});
})();