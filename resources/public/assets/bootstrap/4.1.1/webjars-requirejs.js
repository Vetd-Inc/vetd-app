/*global requirejs */

// Ensure any request for this webjar brings in jQuery.
requirejs.config({
  paths: { 
    "bootstrap": webjars.path("bootstrap", "js/bootstrap"),
    "bootstrap-css": webjars.path("bootstrap", "css/bootstrap")  
  },
  shim: { "bootstrap": [ "jquery" ] }
});

