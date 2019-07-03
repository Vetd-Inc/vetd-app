// Compiled by ClojureScript 1.10.516 {}
goog.provide('vetd_app.core_test');
goog.require('cljs.core');
goog.require('latte.chai');
vetd_app.core_test.cy = cy;
describe("Login",(function (){
before((function (){
vetd_app.core_test.cy.visit("http://localhost:8080");

return null;
}));

it("logs in",(function (){
vetd_app.core_test.cy.get("#name").type("Pikachu");

vetd_app.core_test.cy.get("#submit").click();

vetd_app.core_test.cy.get("#pok\u00E9mon").first().should((function (pokémon){
latte.chai.expect.call(null,pokémon,new cljs.core.Keyword(null,"to.have.length","to.have.length",-1972832085),(1));

return latte.chai.expect.call(null,pokémon,new cljs.core.Keyword(null,"to.contain","to.contain",-2129859892),"Pikachu");
}));

return null;
}));

return null;
}));

//# sourceMappingURL=core_test.js.map
