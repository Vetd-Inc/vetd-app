// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('vetd_app.core_test');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('cljs.test');
vetd_app.core_test.cy = cy;
describe("Login",(function (){
before((function (){
vetd_app.core_test.cy.visit("http://localhost:5080");

return null;
}));

it("logs in",(function (){
vetd_app.core_test.cy.get(".ui.input input").first().type("a@a.com");

vetd_app.core_test.cy.get(".ui.input input").eq((1)).type("aaaaaaaa");

vetd_app.core_test.cy.get("button").contains("Log In").click();

vetd_app.core_test.cy.get(".ui.input");

return null;
}));

return null;
}));
