// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('latte.chai');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('clojure.set');
goog.require('clojure.string');
goog.require('kit.core');
goog.require('latte.add');
goog.require('latte.overwrite');
latte.chai.chai = (cljs.core.truth_(kit.core.module_system_QMARK_())?require("chai"):chai);
latte.chai.expect_STAR_ = (latte.chai.chai["expect"]);
latte.chai.assertion_QMARK_ = (function latte$chai$assertion_QMARK_(x){
return cljs.core._EQ_.cljs$core$IFn$_invoke$arity$2(x,latte.chai.chai.Assertion);
});
latte.chai.function_QMARK_ = (function latte$chai$function_QMARK_(x){
return typeof x === "function";
});
latte.chai.find_meth = (function latte$chai$find_meth(obj,ks){
var obj__$1 = (latte.chai.expect_STAR_.cljs$core$IFn$_invoke$arity$1 ? latte.chai.expect_STAR_.cljs$core$IFn$_invoke$arity$1(obj) : latte.chai.expect_STAR_.call(null,obj));
var ks__$1 = ks;
while(true){
if(cljs.core.truth_(cljs.core.not_empty(ks__$1))){
var temp__5735__auto__ = (obj__$1[cljs.core.name(cljs.core.first(ks__$1))]);
if(cljs.core.truth_(temp__5735__auto__)){
var meth = temp__5735__auto__;
if(cljs.core.truth_((function (){var and__4120__auto__ = latte.chai.function_QMARK_(meth);
if(cljs.core.truth_(and__4120__auto__)){
return cljs.core.empty_QMARK_(cljs.core.rest(ks__$1));
} else {
return and__4120__auto__;
}
})())){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [meth,obj__$1], null);
} else {
var G__5928 = meth;
var G__5929 = cljs.core.rest(ks__$1);
obj__$1 = G__5928;
ks__$1 = G__5929;
continue;
}
} else {
throw (new Error(["No property: ",cljs.core.name(cljs.core.first(ks__$1))," found"].join('')));
}
} else {
return obj__$1;
}
break;
}
});
latte.chai.expect = (function latte$chai$expect(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5936 = arguments.length;
var i__4731__auto___5937 = (0);
while(true){
if((i__4731__auto___5937 < len__4730__auto___5936)){
args__4736__auto__.push((arguments[i__4731__auto___5937]));

var G__5938 = (i__4731__auto___5937 + (1));
i__4731__auto___5937 = G__5938;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((2) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((2)),(0),null)):null);
return latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),argseq__4737__auto__);
});

latte.chai.expect.cljs$core$IFn$_invoke$arity$variadic = (function (obj,expr,args){
var temp__5735__auto__ = latte.chai.find_meth(obj,clojure.string.split.cljs$core$IFn$_invoke$arity$2(cljs.core.name(expr),/\./));
if(cljs.core.truth_(temp__5735__auto__)){
var result = temp__5735__auto__;
if(cljs.core.vector_QMARK_(result)){
var vec__5933 = result;
var meth = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5933,(0),null);
var this$ = cljs.core.nth.cljs$core$IFn$_invoke$arity$3(vec__5933,(1),null);
return meth.apply(this$,cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(args));
} else {
if(cljs.core.truth_(cljs.core.not_empty(args))){
throw (new Error(["Arguments: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(cljs.core.clj__GT_js(args))," supplied to property expression ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(expr)," where none where expected"].join('')));
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
latte.chai.expect.cljs$lang$applyTo = (function (seq5930){
var G__5931 = cljs.core.first(seq5930);
var seq5930__$1 = cljs.core.next(seq5930);
var G__5932 = cljs.core.first(seq5930__$1);
var seq5930__$2 = cljs.core.next(seq5930__$1);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5931,G__5932,seq5930__$2);
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
latte.add.method.cljs$core$IFn$_invoke$arity$variadic(cljs.core.cst$kw$value,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.cst$kw$message,"expected #{act} to contain the value #{exp}",cljs.core.cst$kw$negation,"expected #{act} not to contain the value #{exp}",cljs.core.cst$kw$assertion,cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.boolean$,(function (p1__5940_SHARP_,p2__5939_SHARP_){
return cljs.core.some(cljs.core.PersistentHashSet.createAsIfByAssoc([p2__5939_SHARP_]),cljs.core.seq(p1__5940_SHARP_));
}))], 0));
latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic(cljs.core.cst$kw$equal,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.cst$kw$message,"expected #{exp} to be equal to #{act}",cljs.core.cst$kw$negation,"expected #{exp} not to be equal to #{act}",cljs.core.cst$kw$guard,cljs.core.some_fn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.list_QMARK_,cljs.core.map_QMARK_,cljs.core.vector_QMARK_,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.seq_QMARK_,cljs.core.set_QMARK_], 0)),cljs.core.cst$kw$assertion,cljs.core._EQ_], 0));
latte.overwrite.property.cljs$core$IFn$_invoke$arity$variadic(cljs.core.cst$kw$empty,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.cst$kw$message,"expected #{act} to be empty",cljs.core.cst$kw$negation,"expected #{act} not to be empty",cljs.core.cst$kw$guard,cljs.core.some_fn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.list_QMARK_,cljs.core.map_QMARK_,cljs.core.vector_QMARK_,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.seq_QMARK_,cljs.core.set_QMARK_], 0)),cljs.core.cst$kw$assertion,cljs.core.empty_QMARK_], 0));
latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic(cljs.core.cst$kw$keys,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.cst$kw$message,"expected #{act} to contain keys #{exp}",cljs.core.cst$kw$negation,"expected #{act} not to contain keys #{exp}",cljs.core.cst$kw$guard,cljs.core.map_QMARK_,cljs.core.cst$kw$assertion,(function() { 
var G__5942__delegate = function (m,ks){
return cljs.core.every_QMARK_((function (p1__5941_SHARP_){
return cljs.core.contains_QMARK_(m,p1__5941_SHARP_);
}),cljs.core.flatten(ks));
};
var G__5942 = function (m,var_args){
var ks = null;
if (arguments.length > 1) {
var G__5943__i = 0, G__5943__a = new Array(arguments.length -  1);
while (G__5943__i < G__5943__a.length) {G__5943__a[G__5943__i] = arguments[G__5943__i + 1]; ++G__5943__i;}
  ks = new cljs.core.IndexedSeq(G__5943__a,0,null);
} 
return G__5942__delegate.call(this,m,ks);};
G__5942.cljs$lang$maxFixedArity = 1;
G__5942.cljs$lang$applyTo = (function (arglist__5944){
var m = cljs.core.first(arglist__5944);
var ks = cljs.core.rest(arglist__5944);
return G__5942__delegate(m,ks);
});
G__5942.cljs$core$IFn$_invoke$arity$variadic = G__5942__delegate;
return G__5942;
})()
], 0));
latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic(cljs.core.cst$kw$members,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.cst$kw$message,"expected #{act} to be a superset of #{exp}",cljs.core.cst$kw$negation,"expected #{act} not to be a superset of #{act}",cljs.core.cst$kw$guard,cljs.core.some_fn.cljs$core$IFn$_invoke$arity$variadic(cljs.core.list_QMARK_,cljs.core.vector_QMARK_,cljs.core.seq_QMARK_,cljs.core.prim_seq.cljs$core$IFn$_invoke$arity$2([cljs.core.set_QMARK_], 0)),cljs.core.cst$kw$assertion,(function (x,y){
return cljs.core.empty_QMARK_(clojure.set.difference.cljs$core$IFn$_invoke$arity$2(cljs.core.set(y),cljs.core.set(x)));
})], 0));
