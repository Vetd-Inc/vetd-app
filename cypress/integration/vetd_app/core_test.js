// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('vetd_app.core_test');
goog.require('cljs.core');
goog.require('cljs.core.constants');
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
latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic(pokémon,cljs.core.cst$kw$to$have$length,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([(1)], 0));

return latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic(pokémon,cljs.core.cst$kw$to$contain,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2(["Pikachu"], 0));
}));

return null;
}));

return null;
}));
