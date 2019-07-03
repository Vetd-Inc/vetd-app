// Compiled by ClojureScript 1.10.516 {}
goog.provide('latte.chai');
goog.require('cljs.core');
goog.require('clojure.set');
goog.require('clojure.string');
goog.require('kit.core');
goog.require('latte.add');
goog.require('latte.overwrite');
latte.chai.chai = (cljs.core.truth_(kit.core.module_system_QMARK_.call(null))?require("chai"):chai);
latte.chai.expect_STAR_ = (latte.chai.chai["expect"]);
latte.chai.assertion_QMARK_ = (function latte$chai$assertion_QMARK_(x){
return cljs.core._EQ_.call(null,x,latte.chai.chai.Assertion);
});
latte.chai.function_QMARK_ = (function latte$chai$function_QMARK_(x){
return typeof x === "function";
});
latte.chai.find_meth = (function latte$chai$find_meth(obj,ks){
var obj__$1 = latte.chai.expect_STAR_.call(null,obj);
var ks__$1 = ks;
while(true){
if(cljs.core.truth_(cljs.core.not_empty.call(null,ks__$1))){
var temp__5735__auto__ = (obj__$1[cljs.core.name.call(null,cljs.core.first.call(null,ks__$1))]);
if(cljs.core.truth_(temp__5735__auto__)){
var meth = temp__5735__auto__;
if(cljs.core.truth_((function (){var and__4120__auto__ = latte.chai.function_QMARK_.call(null,meth);
if(cljs.core.truth_(and__4120__auto__)){
return cljs.core.empty_QMARK_.call(null,cljs.core.rest.call(null,ks__$1));
} else {
return and__4120__auto__;
}
})())){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [meth,obj__$1], null);
} else {
var G__5278 = meth;
var G__5279 = cljs.core.rest.call(null,ks__$1);
obj__$1 = G__5278;
ks__$1 = G__5279;
continue;
}
} else {
throw (new Error(["No property: ",cljs.core.name.call(null,cljs.core.first.call(null,ks__$1))," found"].join('')));
}
} else {
return obj__$1;
}
break;
}
});
latte.chai.expect = (function latte$chai$expect(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5286 = arguments.length;
var i__4731__auto___5287 = (0);
while(true){
if((i__4731__auto___5287 < len__4730__auto___5286)){
args__4736__auto__.push((arguments[i__4731__auto___5287]));

var G__5288 = (i__4731__auto___5287 + (1));
i__4731__auto___5287 = G__5288;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((2) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((2)),(0),null)):null);
return latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__4737__auto__);
});

latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic = (function (obj,expr,args){
var temp__5735__auto__ = latte.chai.find_meth.call(null,obj,clojure.string.split.call(null,cljs.core.name.call(null,expr),/\./));
if(cljs.core.truth_(temp__5735__auto__)){
var result = temp__5735__auto__;
if(cljs.core.vector_QMARK_.call(null,result)){
var vec__5283 = result;
var meth = cljs.core.nth.call(null,vec__5283,(0),null);
var this$ = cljs.core.nth.call(null,vec__5283,(1),null);
return meth.apply(this$,cljs.core.into_array.call(null,args));
} else {
if(cljs.core.truth_(cljs.core.not_empty.call(null,args))){
throw (new Error(["Arguments: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.clj__GT_js.call(null,args))," supplied to property expression ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(expr)," where none where expected"].join('')));
} else {
return null;
}
}
} else {
throw (new Error("Could not find test method"));
}
});

latte.chai.expect.cljs$lang$maxFixedArity = (2);

/** @this {Function} */
latte.chai.expect.cljs$lang$applyTo = (function (seq5280){
var G__5281 = cljs.core.first.call(null,seq5280);
var seq5280__$1 = cljs.core.next.call(null,seq5280);
var G__5282 = cljs.core.first.call(null,seq5280__$1);
var seq5280__$2 = cljs.core.next.call(null,seq5280__$1);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5281,G__5282,seq5280__$2);
});

latte.chai.plugin = (function latte$chai$plugin(s){
var temp__5737__auto__ = require(s);
if(cljs.core.truth_(temp__5737__auto__)){
var module = temp__5737__auto__;
latte.chai.chai.use(module);

return module;
} else {
return null;
}
});
latte.add.method.call(null,new cljs.core.Keyword(null,"value","value",305978217),new cljs.core.Keyword(null,"message","message",-406056002),"expected #{act} to contain the value #{exp}",new cljs.core.Keyword(null,"negation","negation",-755634643),"expected #{act} not to contain the value #{exp}",new cljs.core.Keyword(null,"assertion","assertion",-1645134882),cljs.core.comp.call(null,cljs.core.boolean$,(function (p1__5290_SHARP_,p2__5289_SHARP_){
return cljs.core.some.call(null,cljs.core.PersistentHashSet.createAsIfByAssoc([p2__5289_SHARP_]),cljs.core.seq.call(null,p1__5290_SHARP_));
})));
latte.overwrite.method.call(null,new cljs.core.Keyword(null,"equal","equal",-1921681350),new cljs.core.Keyword(null,"message","message",-406056002),"expected #{exp} to be equal to #{act}",new cljs.core.Keyword(null,"negation","negation",-755634643),"expected #{exp} not to be equal to #{act}",new cljs.core.Keyword(null,"guard","guard",-873147811),cljs.core.some_fn.call(null,cljs.core.list_QMARK_,cljs.core.map_QMARK_,cljs.core.vector_QMARK_,cljs.core.seq_QMARK_,cljs.core.set_QMARK_),new cljs.core.Keyword(null,"assertion","assertion",-1645134882),cljs.core._EQ_);
latte.overwrite.property.call(null,new cljs.core.Keyword(null,"empty","empty",767870958),new cljs.core.Keyword(null,"message","message",-406056002),"expected #{act} to be empty",new cljs.core.Keyword(null,"negation","negation",-755634643),"expected #{act} not to be empty",new cljs.core.Keyword(null,"guard","guard",-873147811),cljs.core.some_fn.call(null,cljs.core.list_QMARK_,cljs.core.map_QMARK_,cljs.core.vector_QMARK_,cljs.core.seq_QMARK_,cljs.core.set_QMARK_),new cljs.core.Keyword(null,"assertion","assertion",-1645134882),cljs.core.empty_QMARK_);
latte.overwrite.method.call(null,new cljs.core.Keyword(null,"keys","keys",1068423698),new cljs.core.Keyword(null,"message","message",-406056002),"expected #{act} to contain keys #{exp}",new cljs.core.Keyword(null,"negation","negation",-755634643),"expected #{act} not to contain keys #{exp}",new cljs.core.Keyword(null,"guard","guard",-873147811),cljs.core.map_QMARK_,new cljs.core.Keyword(null,"assertion","assertion",-1645134882),(function() { 
var G__5292__delegate = function (m,ks){
return cljs.core.every_QMARK_.call(null,(function (p1__5291_SHARP_){
return cljs.core.contains_QMARK_.call(null,m,p1__5291_SHARP_);
}),cljs.core.flatten.call(null,ks));
};
var G__5292 = function (m,var_args){
var ks = null;
if (arguments.length > 1) {
var G__5293__i = 0, G__5293__a = new Array(arguments.length -  1);
while (G__5293__i < G__5293__a.length) {G__5293__a[G__5293__i] = arguments[G__5293__i + 1]; ++G__5293__i;}
  ks = new cljs.core.IndexedSeq(G__5293__a,0,null);
} 
return G__5292__delegate.call(this,m,ks);};
G__5292.cljs$lang$maxFixedArity = 1;
G__5292.cljs$lang$applyTo = (function (arglist__5294){
var m = cljs.core.first(arglist__5294);
var ks = cljs.core.rest(arglist__5294);
return G__5292__delegate(m,ks);
});
G__5292.cljs$core$IFn$_invoke$arity$variadic = G__5292__delegate;
return G__5292;
})()
);
latte.overwrite.method.call(null,new cljs.core.Keyword(null,"members","members",159001018),new cljs.core.Keyword(null,"message","message",-406056002),"expected #{act} to be a superset of #{exp}",new cljs.core.Keyword(null,"negation","negation",-755634643),"expected #{act} not to be a superset of #{act}",new cljs.core.Keyword(null,"guard","guard",-873147811),cljs.core.some_fn.call(null,cljs.core.list_QMARK_,cljs.core.vector_QMARK_,cljs.core.seq_QMARK_,cljs.core.set_QMARK_),new cljs.core.Keyword(null,"assertion","assertion",-1645134882),(function (x,y){
return cljs.core.empty_QMARK_.call(null,clojure.set.difference.call(null,cljs.core.set.call(null,y),cljs.core.set.call(null,x)));
}));
