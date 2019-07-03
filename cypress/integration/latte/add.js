// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('latte.add');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('kit.core');
latte.add.chai = (cljs.core.truth_(kit.core.module_system_QMARK_())?require("chai"):chai);
latte.add.assertions = (latte.add.chai["Assertion"]);
latte.add.method = (function latte$add$method(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5872 = arguments.length;
var i__4731__auto___5873 = (0);
while(true){
if((i__4731__auto___5873 < len__4730__auto___5872)){
args__4736__auto__.push((arguments[i__4731__auto___5873]));

var G__5874 = (i__4731__auto___5873 + (1));
i__4731__auto___5873 = G__5874;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.add.method.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.add.method.cljs$core$IFn$_invoke$arity$variadic = (function (k,args){
var args__$1 = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,args);
var assertion = cljs.core.cst$kw$assertion.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly(false));
var expected = cljs.core.cst$kw$expected.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.clj__GT_js,cljs.core.first));
return latte.add.assertions.addMethod(cljs.core.name(k),((function (args__$1,assertion,expected){
return (function() { 
var G__5875__delegate = function (expectations){
var this$ = this;
var obj = (this$["_obj"]);
return this$.assert(cljs.core.apply.cljs$core$IFn$_invoke$arity$3(assertion,obj,expectations),cljs.core.cst$kw$message.cljs$core$IFn$_invoke$arity$1(args__$1),cljs.core.cst$kw$negation.cljs$core$IFn$_invoke$arity$1(args__$1),(expected.cljs$core$IFn$_invoke$arity$1 ? expected.cljs$core$IFn$_invoke$arity$1(expectations) : expected.call(null,expectations)),cljs.core.clj__GT_js(obj));
};
var G__5875 = function (var_args){
var expectations = null;
if (arguments.length > 0) {
var G__5876__i = 0, G__5876__a = new Array(arguments.length -  0);
while (G__5876__i < G__5876__a.length) {G__5876__a[G__5876__i] = arguments[G__5876__i + 0]; ++G__5876__i;}
  expectations = new cljs.core.IndexedSeq(G__5876__a,0,null);
} 
return G__5875__delegate.call(this,expectations);};
G__5875.cljs$lang$maxFixedArity = 0;
G__5875.cljs$lang$applyTo = (function (arglist__5877){
var expectations = cljs.core.seq(arglist__5877);
return G__5875__delegate(expectations);
});
G__5875.cljs$core$IFn$_invoke$arity$variadic = G__5875__delegate;
return G__5875;
})()
;})(args__$1,assertion,expected))
);
});

latte.add.method.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.add.method.cljs$lang$applyTo = (function (seq5870){
var G__5871 = cljs.core.first(seq5870);
var seq5870__$1 = cljs.core.next(seq5870);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5871,seq5870__$1);
});

