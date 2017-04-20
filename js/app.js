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
      });
    
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
      });
  }]);

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
      	});
    };
    
  });
  
  app.controller('HomeController', function($scope, $rootScope, $stateParams, $state, LoginService) {
    $rootScope.title = "Home";
    $scope.userData = $stateParams.userData;
    console.log("user info = ", $scope.userData);
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
  
})();