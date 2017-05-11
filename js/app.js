(function() {
	// Initialize our app module
	var app = angular.module('FabFlix', ['ui.router','ngCookies','vcRecaptcha']);
  
  // http://stackoverflow.com/questions/19254029/angularjs-http-post-does-not-send-data/35699599
	angular.module('FabFlix')
		.config(function ($httpProvider, $httpParamSerializerJQLikeProvider){
			$httpProvider.defaults.transformRequest.unshift($httpParamSerializerJQLikeProvider.$get());
			$httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded; charset=utf-8';
	});
	
	// Initializing our app (rootScope)
	app.run(function($rootScope, $location, $state, $cookies, LoginService) {
		$rootScope.$on('$stateChangeStart', 
			function(event, toState, toParams, fromState, fromParams){ 
			}
		);
		
		// If the user refreshes the page. take them back to login
		$rootScope.$on("$locationChangeStart", function(event, next, current) { 
			if(next == current) {
				// If user is still logged in, keep in home if they decide to refresh
				if (!$rootScope.user && next != '/login') {
					event.preventDefault();
					
					if (!$cookies.getObject("user")) {
						$state.transitionTo('login');
						return;
					}
					
					$rootScope.user = $cookies.getObject('user');
					$state.go('home', {userData: $rootScope.user});
					return;
				}
			}
		});
		
		$rootScope.offset = 0;	// by default, we look at the first search results from the home controller
		$rootScope.limit = 10;	// by default, we limit the search results to 10 per page from the home controller
			
		if(!LoginService.isAuthenticated()) {
			$state.transitionTo('login');
		}
	});
  
	app.config(['$stateProvider', '$urlRouterProvider', function($stateProvider, $urlRouterProvider) {
		$urlRouterProvider.otherwise('/login');

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
				params: {
					userData: null,
					confirmationMessage: null,
					confirmationMessageClass: null
				}
			})
			.state('results', {
				url : '/results?query_type={}title={}year={}director={}starFirstName={}starLastName={}genre={}starts_with={}order_type={}order_by={}offset={}limit={}',
				templateUrl : 'html/results.html',
				controller : 'ResultsController',
				params: {
					userData: null,
					query_display: null, 
					query_type: null,
					starts_with: null,
					genre: null,
					title: null,
					year: null,
					director: null,
					starFirstName: null,
					starLastName: null,
					order_by: null,
					order_type: null,
					offset: null,
					limit: null
				}
			})
			.state('movie', {
				url : '/movie?movieId={}',
				templateUrl : 'html/movie.html',
				controller : 'MovieController',
				params: {
					userData: null,
					movieId: null
				}
			})
			.state('star', {
				url : '/star?starId={}',
				templateUrl : 'html/star.html',
				controller : 'StarController',
				params: {
					userData: null,
					starId: null
				}
			})
			.state('cart', {
				url : '/cart',
				templateUrl : 'html/cart.html',
				controller : 'CartController',
				params: {
					userData: null
				}
			})
			.state('checkout', {
				url : '/checkout',
				templateUrl : 'html/checkout.html',
				controller : 'CheckoutController',
				params: {
					userData: null,
					cartData: null
				}
			})
			.state('readme', {
				url: '/reports/readme',
				templateUrl: 'reports/readme'
			})
			.state('like-predicate', {
				url: '/reports/like-predicate',
				templateUrl: 'reports/like-predicate'
			})
		}]
	);

	app.controller('LoginController', function($scope, $rootScope, $stateParams, $state, $http, $cookies, $window, vcRecaptchaService, LoginService) {
		$rootScope.title = "FabFlix Login";
		$scope.loggingIn = false;
		$cookies.putObject('user', null);

		// Called after user attempts to log in
		$scope.formSubmit = function() {
			$scope.loggingIn = true;
  
			LoginService.login($http, $scope.email, $scope.password, $scope.recaptchaResponse)
				.then(function(data) {
					if (data && data.error == "null" && data.message == "success" && data.user != "null") {
						$scope.error = '';
						$scope.email = '';
						$scope.password = '';
						$rootScope.user = data.user;
						$cookies.putObject('user', $rootScope.user);
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
		$scope.showError = false;
		
		$scope.confirmationMessage = $stateParams.confirmationMessage;	// for when user comes from a checkout page
		
		if ($stateParams.confirmationMessageClass)
			angular.element( document.querySelector( '#confirmationText' ) )
				.addClass($stateParams.confirmationMessageClass);
		
		$scope.searchSubmit = function() {
			$scope.showError = !$scope.movieTitle && !$scope.year 
				&& !$scope.director 
				&& !$scope.starFirstName && !$scope.starLastName;
			
			if (!$scope.showError) {
				$state.go('results', {
					userData: $scope.userData,
					query_type: "BROWSE_BY_PARAMETERS",
					title: $scope.movieTitle,
					year: $scope.year,
					director: $scope.director,
					starFirstName: $scope.starFirstName,
					starLastName: $scope.starLastName,
					order_by: "title",	// by default
					order_type: "asc",	// by default
					offset: $rootScope.offset,
					limit: $rootScope.limit,
					query_display: "Movies " + // IMPORTANT: For displaying proper response when no results found!
						(!$scope.movieTitle ? "" : "with title '" + $scope.movieTitle + "'") + 
						(!$scope.year ? "" : " with year '" + $scope.year + "'") + 
						(!$scope.director ? "" : " with director '" + $scope.director + "'") + 
						(!$scope.starFirstName ? "" : " with star's first name '" + $scope.starFirstName + "'") + 
						(!$scope.starLastName ? "" : " with star's last name '" + $scope.starLastName + "'")
				});
			}
		}
		
		/**
		 * Get search results for movies by first character in movie title
		 */
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
		
		/**
		 * Searches for movies with a given genre
		 */
		$scope.browseByMovieGenre = function(genre) {
			if (!genre)
				return;
			
			$state.go('results', {
				userData: $scope.userData,
				query_type: "BROWSE_BY_MOVIE_GENRE",
				genre: genre,
				order_by: "title",	// by default
				order_type: "asc",	// by default
				offset: $rootScope.offset,
				limit: $rootScope.limit,
				query_display: "Movies with genre '" + genre + "'"	// IMPORTANT: For displaying proper response when no results found!
			});
		}
	});
	
	/**
	 * HTML File: html/results.html
	 */
	app.controller('ResultsController', function($scope, $rootScope, $stateParams, $state, $http, SearchService, CartService) {
		$rootScope.title = "Results";
		
		$scope.loading = true;
		$scope.errorName = "";
		$scope.errorMessage = "";
		
		$scope.userData = $stateParams.userData;
		$scope.query_type = $stateParams.query_type;
		$scope.query_display = $stateParams.query_display;
		$scope.order_by = $stateParams.order_by;
		$scope.order_type = $stateParams.order_type;
		$scope.offset = Number($stateParams.offset);
		$scope.limit = $stateParams.limit;
		
		// Get parameters for browsing by first character in title
		$scope.starts_with = $stateParams.starts_with;
		
		// Get parameters for browsing by genre
		$scope.genre = $stateParams.genre;
		
		$scope.title = $stateParams.title;
		$scope.year = $stateParams.year;
		$scope.director = $stateParams.director;
		$scope.starFirstName = $stateParams.starFirstName;
		$scope.starLastName = $stateParams.starLastName;
		
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
					$scope.starts_with, 
					$scope.order_by, $scope.order_type,
					$scope.offset, $scope.limit)
				.then(function(data) {
					if (data) {
						if (data.error) {
							$scope.error = data.error;
							$scope.errorMessage = data.message;
							$scope.loading = false;
							return;
						}
						
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
						
						// Set the default values for cart options for each movie
						$scope.data.forEach(function(movie) {
							if (!movie.movie_cart_quantity)
								movie.movie_cart_quantity = 0;
							movie.addToCartText = "Add to Cart";
							movie.loadingCartSubmit = false;
						});
					} else {
						$scope.data = null;
						$scope.number_of_results = 0;
					}
					$scope.loading = false;
				}
			);
		}
		if ($stateParams.query_type == "BROWSE_BY_MOVIE_GENRE") {
			SearchService.browseByMovieGenre($http, 
					$scope.genre, 
					$scope.order_by, $scope.order_type,
					$scope.offset, $scope.limit)
				.then(function(data) {		
					if (data) {
						if (data.error) {
							$scope.error = data.error;
							$scope.errorMessage = data.message;
							$scope.loading = false;
							return;
						}
						
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
						
						// Set the default values for cart options for each movie
						$scope.data.forEach(function(movie) {
							if (!movie.movie_cart_quantity)
								movie.movie_cart_quantity = 0;
							movie.addToCartText = "Add to Cart";
							movie.loadingCartSubmit = false;
						});
					} else {
						$scope.data = null;
						$scope.number_of_results = 0;
					}
					$scope.loading = false;
				}
			);
		}
		if ($stateParams.query_type == "BROWSE_BY_PARAMETERS") {
			SearchService.searchByMovieParameters($http, 
					$scope.title,
					$scope.year,
					$scope.director,
					$scope.starFirstName, $scope.starLastName,
					$scope.order_by, $scope.order_type, $scope.offset, $scope.limit)
				.then(function(data) {		
					if (data) {
						if (data.error) {
							$scope.error = data.error;
							$scope.errorMessage = data.message;
							$scope.loading = false;
							return;
						}
						
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
						
						// Set the default values for cart options for each movie
						$scope.data.forEach(function(movie) {
							if (!movie.movie_cart_quantity)
								movie.movie_cart_quantity = 0;
							movie.addToCartText = "Add to Cart";
							movie.loadingCartSubmit = false;
						});
						
					} else {
						$scope.data = null;
						$scope.number_of_results = 0;
					}
					$scope.loading = false;
				}
			);
		}
		
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.addCartQuantity = function(movie) {
			if (isNaN(movie.movie_cart_quantity))
				movie.movie_cart_quantity = 0;
			movie.movie_cart_quantity = Math.max(parseInt(movie.movie_cart_quantity) + 1, 0);
		}
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.subtractCartQuantity = function(movie) {
			if (isNaN(movie.movie_cart_quantity))
				movie.movie_cart_quantity = 0;
			movie.movie_cart_quantity = Math.max(parseInt(movie.movie_cart_quantity) - 1, 0);
		}
		
		$scope.submitCartQuantity = function(movie) {
			if (isNaN(movie.movie_cart_quantity))
				movie.movie_cart_quantity = 0;
			console.log("cart quant = ", movie.movie_cart_quantity);
			movie.loadingCartSubmit = true;
			movie.addToCartText = "Adding to Cart";
			
			// Update the backend table for the user's cart
			CartService.updateCartData($http, movie.movie_id, movie.movie_cart_quantity)
				.then(function(success) {
					if (success) {
						if ($scope.previousMovieUpdated)
							$scope.previousMovieUpdated.showUpdate = false;
						
						$scope.previousMovieUpdated = movie;
						movie.showUpdate = true;
					}
					movie.loadingCartSubmit = false; 
					movie.addToCartText = "Add to Cart";
				}
			);
		}
		
		/**
		 * Searches for movies with a given genre
		 */
		$scope.browseByMovieGenre = function(genre) {
			if (!genre)
				return;
			
			$state.go('results', {
				userData: $scope.userData,
				query_type: "BROWSE_BY_MOVIE_GENRE",
				genre: genre,
				order_by: "title",	// by default
				order_type: "asc",	// by default
				offset: $rootScope.offset,
				limit: $rootScope.limit,
				query_display: "Movies with genre '" + genre + "'"	// IMPORTANT: For displaying proper response when no results found!
			});
		}
		
		/**
		 * Refresh the search with new order column (title, year)
		 */
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
		
		/**
		 * Refresh the search with new order type (descending, ascending)
		 */
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
		
		/**
		 * Refresh the search with new search limit
		 */
		$scope.adjustSearchLimit = function(new_limit) {
			$state.go('results', {
				userData: $scope.userData,
				query_type: $scope.query_type,
				starts_with: $scope.starts_with,
				order_by: $scope.order_by,
				order_type: $scope.order_type,
				offset: 0,
				limit: new_limit,
				query_display: $scope.query_display
			});
		}
		
		/**
		 * Get the next set of search results 
		 */
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
		
		/**
		 * Get the previous set of search results
		 */
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
		
		/**
		 * Go to the movie page for the clicked movie
		 */
		$scope.onMovieClick = function(movie_id) {
			$state.go('movie', {
				userData: $scope.userData,
				movieId: movie_id
			});
		}
		
		$scope.onStarClicked = function(star_id) {
			$state.go('star', {
				userData: $scope.userData,
				starId: star_id
			});
		}
	});
	
	app.controller('MovieController', function($scope, $rootScope, $stateParams, $state, $http, $window, SearchService, CartService) {
		$rootScope.title = "Movie";
		$scope.userData = $stateParams.userData;
		$scope.movieId = $stateParams.movieId;
		$scope.movieData = null;
		$scope.loading = true;
		
		$scope.addToCartText = "Add to Cart";
		$scope.loadingCartSubmit = false;	// is true after clicking "Add to Cart" button
		
		SearchService.getMovieById($http, $scope.movieId)
			.then(function(data) {
				if (data) {
					$scope.movieData = data;
					
				} else {
					$scope.movieData = null;
				}
				$scope.loading = false;
			}
		);
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.addCartQuantity = function() {
			if (isNaN($scope.movieData.movie_cart_quantity))
				$scope.movieData.movie_cart_quantity = 0;
			$scope.movieData.movie_cart_quantity = Math.max(parseInt($scope.movieData.movie_cart_quantity) + 1, 0);
		}
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.subtractCartQuantity = function() {
			if (isNaN($scope.movieData.movie_cart_quantity))
				$scope.movieData.movie_cart_quantity = 0;
			$scope.movieData.movie_cart_quantity = Math.max(parseInt($scope.movieData.movie_cart_quantity) - 1, 0);
		}
		
		$scope.submitCartQuantity = function() {
			if (isNaN($scope.movieData.movie_cart_quantity))
				$scope.movieData.movie_cart_quantity = 0;
			$scope.loadingCartSubmit = true;
			$scope.addToCartText = "Adding to Cart";
			
			// Update the backend table for the user's cart
			CartService.updateCartData($http, $scope.movieData.movie_id, $scope.movieData.movie_cart_quantity)
				.then(function(success) {
					if (success) {
						$scope.successUpdateMovie = $scope.movieData.movie_title;
					}
					$scope.loadingCartSubmit = false; 
					$scope.addToCartText = "Add to Cart";
				}
			);
		}
		
		/**
		 * Searches for movies with a given genre
		 */
		$scope.browseByMovieGenre = function(genre) {
			if (!genre)
				return;
			
			$state.go('results', {
				userData: $scope.userData,
				query_type: "BROWSE_BY_MOVIE_GENRE",
				genre: genre,
				order_by: "title",	// by default
				order_type: "asc",	// by default
				offset: $rootScope.offset,
				limit: $rootScope.limit,
				query_display: "Movies with genre '" + genre + "'"	// IMPORTANT: For displaying proper response when no results found!
			});
		}
		
		$scope.onPreviewLinkClicked = function() {
			$window.open($scope.movieData.movie_trailer_url, '_blank');
		}
		
		$scope.onStarClicked = function(star_id) {
			$state.go('star', {
				userData: $scope.userData,
				starId: star_id
			});
		}
		
		$scope.backClicked = function() {
			$window.history.back();
		}
	});
	
	app.controller('StarController', function($scope, $rootScope, $stateParams, $state, $http, $window, SearchService) {
		$rootScope.title = "Star";
		$scope.userData = $stateParams.userData;
		$scope.starId = $stateParams.starId;
		$scope.starData = null;
		$scope.loading = true;
		
		SearchService.getStarById($http, $scope.starId)
			.then(function(data) {
				if (data) {
					$scope.starData = data;
					
				} else {
					$scope.starData = null;
				}
				$scope.loading = false;
			}
		);
		
		$scope.onMovieClick = function(movie_id) {
			$state.go('movie', {
				userData: $scope.userData,
				movieId: movie_id
			});
		}
		
		$scope.backClicked = function() {
			$window.history.back();
		}
	});
	
	app.controller('CartController', function($scope, $rootScope, $stateParams, $state, $http, $window, CartService) {
		$rootScope.title = "Cart";
		$scope.userData = $stateParams.userData;
		
		// Load the cart data from the back end
		$scope.cartLoading = true;
		CartService.getCartData($http)
			.then(function(data) {
				if (data) {
					$scope.cartData = data.cartData;
					
				} else {
					$scope.cartData = null;
				}
				$scope.cartLoading = false;
			}
		);
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.addCartQuantity = function(movie) {
			if (isNaN(movie.movie_cart_quantity))
				movie.movie_cart_quantity = movie.movie_cart_quantity;
			movie.movie_cart_quantity = Math.max(parseInt(movie.movie_cart_quantity) + 1, 0);
		}
		
		/**
		 * Adds to the cart quantity for a movie
		 */
		$scope.subtractCartQuantity = function(movie) {
			if (isNaN(movie.movie_cart_quantity))
				movie.movie_cart_quantity = movie.movie_cart_quantity;
			movie.movie_cart_quantity = Math.max(parseInt(movie.movie_cart_quantity) - 1, 0);
		}
		
		$scope.updateCart = function(movie) {
			// Remove the movie if the quantity given was 0
			if (movie.movie_cart_quantity == 0) {
				return $scope.removeFromCart(movie);
			}
			
			// Update the backend table for the user's cart
			CartService.updateCartData($http, movie.movie_id, movie.movie_cart_quantity)
				.then(function(success) {
					if (success) {
						// Hide any previous alerts if there were any
						$scope.removedMovieTitle = null;
						$scope.removedMovieID = null;
						
						// Show new alert for updated movie
						$scope.updatedMovieTitle = movie.movie_title;
						$scope.updatedMovieID = movie.movie_id;
					}
				}
			);
		}
		
		/**
		 * Removes a movie from the cart
		 */
		$scope.removeFromCart = function(movie) {
			// Update the backend table for the user's cart
			CartService.updateCartData($http, movie.movie_id, 0)
				.then(function(success) {
					if (success) {
						// Hide any previous alerts if there were any
						$scope.updatedMovieTitle = null;
						$scope.updatedMovieID = null;
						
						// Show new alert for removed movie
						$scope.removedMovieTitle = movie.movie_title;
						$scope.removedMovieID = movie.movie_id;
					}
				}
			);
			
			// Simulates removing a movie from the cart
			var index = $scope.cartData.indexOf(movie);
			if (index > -1) {
				$scope.cartData.splice(index, 1);
				

			}
		}
		
		/**
		 * Go to the movie page for the clicked movie
		 */
		$scope.onMovieClick = function(movie_id) {
			$state.go('movie', {
				userData: $scope.userData,
				movieId: movie_id
			});
		}
		
		$scope.onProceedToCheckoutClick = function() {
			// Go to checkout page here!
			$state.go('checkout', {
				userData: $scope.userData,
				cartData: $scope.cartData
			});
		}
	});
	
	
	app.controller('CheckoutController', function($scope, $rootScope, $stateParams, $state, $http, $window, CartService) {
		$rootScope.title = "Checkout";
		$scope.userData = $stateParams.userData;
		
		$scope.errorFieldEmpty = false;	// error for not filling out all fields
		$scope.errorCheckout = false;	// error for unsuccessful checkout
		$scope.loadingCheckout = false;	// to display progress spinner on checkout
		
		// by default, options for expiration date of credit card
		$scope.expirationYear = "2005";
		$scope.expirationMonth = "01";
		$scope.expirationDay = "01";
		
		$scope.cartData = $stateParams.cartData;
		
		
		$scope.onCheckoutSubmit = function() {
			if (!($scope.firstName && $scope.lastName && $scope.creditCardNumber)) {
				$scope.errorFieldEmpty = true;
				return;
			}
			
			$scope.errorFieldEmpty = false;
			
			// TODO: Do HTTP POST here for sending info to backend
			$scope.loadingCheckout = true;
			CartService.validateCreditCardInfo($http, 
					$scope.firstName, $scope.lastName, 
					$scope.creditCardNumber, 
					$scope.expirationYear + "-" + $scope.expirationMonth + "-" + $scope.expirationDay)
				.then(function(data) {
					if (data) {
						if (data.success) {
							var today = new Date();
							var totalTransactionsToDo = 0;
							var totalTransactionsAttempted = 0;
							var confirmationMessageClass = "alert-success";
							
							$scope.cartData.forEach(function(movie) {
								totalTransactionsToDo += movie.movie_cart_quantity;
							});
							var confirmationMessage = "You have completed your transaction of " + totalTransactionsToDo + " movies!"; 
							
							$scope.cartData.forEach(function(movie) {
								for (var i = 0; i < movie.movie_cart_quantity; i++) {
									CartService.submitCartData($http,
											$rootScope.user.id,
											movie.movie_id,
											today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate())
										.then(function(transactionData) {
											totalTransactionsAttempted++;
											
											if (!transactionData || (transactionData && !transactionData.success)) {
												confirmationMessageClass = "alert-danger";
												confirmationMessage = "You may have one or more uncompleted transactions, so please check your cart to see the leftover movies.";
											}
											if (totalTransactionsAttempted == totalTransactionsToDo) {
												$scope.loadingCheckout = false;
												$state.go('home', {
													confirmationMessageClass: confirmationMessageClass,
													confirmationMessage: confirmationMessage
												});
											}
										}
									);
								}
							});
						} else {
							$scope.wrongInformationErrorMessage = data.message;
							$scope.loadingCheckout = false;
						}
						
					} else {
						$scope.noResponseError = true;
						$scope.loadingCheckout = false;
					}
				}
			);
			// TODO: If failure, show error at the top of this screen
			// TODO: If success, lead to confirmation page
		}
	});
  
	app.factory('LoginService', function() {
		var data = null;
  
		return {
			login : function($http, email, password, recaptchaResponse) {
				// HTTP POST runs asnychronously
				return $http({
					method: 'POST',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixLoginServlet',
					data: { email: email, password: password, recaptchaResponse: (recaptchaResponse ? recaptchaResponse : null) }
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
	
	app.factory('CartService', function() {
		return {
			getCartData: function($http) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixCartServlet',	
					params: { 
						action_type: "GET_CART"
					}
				}).then(function successCallback(response) {
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			},
			updateCartData: function($http, movieID, amount) {
				return $http({
					method: 'POST',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixCartServlet',	
					params: { 
						action_type: "UPDATE_CART",
						movieId: movieID,
						amount: amount
					}
				}).then(function successCallback(response) {
					return response.data.success;
				}, function errorCallback(response) {
					return null;
				});
			},
			validateCreditCardInfo: function($http, firstName, lastName, creditCardNumber, creditCardExpiration) {
				return $http({
					method: 'POST',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixCartServlet',	
					params: { 
						action_type: "VALIDATE_CREDIT_CARD",
						first_name: firstName,
						last_name: lastName,
						credit_card_number: creditCardNumber,
						credit_card_expiration: creditCardExpiration
					}
				}).then(function successCallback(response) {
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			},
			submitCartData: function($http, customerId, movieId, transactionDate) {
				return $http({
					method: 'POST',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixCartServlet',	
					params: { 
						action_type: "CART_TRANSACTION",
						customerId: customerId,
						movieId: movieId,
						transactionDate: transactionDate
					}
				}).then(function successCallback(response) {
					console.log("response = ", response);
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			}
		};
	});
  
	app.factory('SearchService', function() {
		return {
			searchByMovieParameters: function($http, title, year, director, 
					starFirstName, starLastName, 
					order_by, order_type, offset, limit ) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixSearchServlet',	
					params: { 
						query_type: "BROWSE_BY_PARAMETERS",
						title: title,
						year: year,
						director: director,
						starFirstName: starFirstName,
						starLastName: starLastName,
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
			},
			
			browseByMovieTitle: function($http, char, order_by, order_type, offset, limit) {
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
			},
			
			browseByMovieGenre: function($http, genre, order_by, order_type, offset, limit) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixSearchServlet',	
					params: { 
						query_type: "BROWSE_BY_MOVIE_GENRE",
						genre: genre,
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
			},
			
			getMovieById: function($http, id) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixMovieServlet',	
					params: { 
						movieId: id
					}
				// IMPORTANT: query_type parameter determines the type of GET operation to use!
				}).then(function successCallback(response) {
					return response.data;
				}, function errorCallback(response) {
					return null;
				});
			},
			
			getStarById: function($http, id) {
				// HTTP GET runs asnychronously
				return $http({
					method: 'GET',
					url: 'http://localhost:8080/FabFlix/servlet/FabFlixStarServlet',	
					params: { 
						star_id: id
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