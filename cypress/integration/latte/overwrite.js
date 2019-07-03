// Compiled by ClojureScript 1.10.516 {:static-fns true, :optimize-constants true}
goog.provide('latte.overwrite');
goog.require('cljs.core');
goog.require('cljs.core.constants');
goog.require('kit.core');
latte.overwrite.chai = (cljs.core.truth_(kit.core.module_system_QMARK_())?require("chai"):chai);
latte.overwrite.assertions = (latte.overwrite.chai["Assertion"]);
latte.overwrite.method = (function latte$overwrite$method(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5882 = arguments.length;
var i__4731__auto___5883 = (0);
while(true){
if((i__4731__auto___5883 < len__4730__auto___5882)){
args__4736__auto__.push((arguments[i__4731__auto___5883]));

var G__5884 = (i__4731__auto___5883 + (1));
i__4731__auto___5883 = G__5884;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic = (function (n,args){
var args__$1 = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,args);
var guard = cljs.core.cst$kw$guard.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly(false));
var assertion = cljs.core.cst$kw$assertion.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly(false));
var expected = cljs.core.cst$kw$expected.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.comp.cljs$core$IFn$_invoke$arity$2(cljs.core.clj__GT_js,cljs.core.first));
return latte.overwrite.assertions.overwriteMethod(cljs.core.name(n),((function (args__$1,guard,assertion,expected){
return (function (super$){
return ((function (args__$1,guard,assertion,expected){
return (function() { 
var G__5885__delegate = function (expectations){
var this$ = this;
var obj = (this$["_obj"]);
if(cljs.core.truth_((guard.cljs$core$IFn$_invoke$arity$1 ? guard.cljs$core$IFn$_invoke$arity$1(obj) : guard.call(null,obj)))){
return this$.assert(cljs.core.apply.cljs$core$IFn$_invoke$arity$3(assertion,obj,expectations),cljs.core.cst$kw$message.cljs$core$IFn$_invoke$arity$1(args__$1),cljs.core.cst$kw$negation.cljs$core$IFn$_invoke$arity$1(args__$1),(expected.cljs$core$IFn$_invoke$arity$1 ? expected.cljs$core$IFn$_invoke$arity$1(expectations) : expected.call(null,expectations)),cljs.core.clj__GT_js(obj));
} else {
return super$.apply(this$,cljs.core.into_array.cljs$core$IFn$_invoke$arity$1(expectations));
}
};
var G__5885 = function (var_args){
var expectations = null;
if (arguments.length > 0) {
var G__5886__i = 0, G__5886__a = new Array(arguments.length -  0);
while (G__5886__i < G__5886__a.length) {G__5886__a[G__5886__i] = arguments[G__5886__i + 0]; ++G__5886__i;}
  expectations = new cljs.core.IndexedSeq(G__5886__a,0,null);
} 
return G__5885__delegate.call(this,expectations);};
G__5885.cljs$lang$maxFixedArity = 0;
G__5885.cljs$lang$applyTo = (function (arglist__5887){
var expectations = cljs.core.seq(arglist__5887);
return G__5885__delegate(expectations);
});
G__5885.cljs$core$IFn$_invoke$arity$variadic = G__5885__delegate;
return G__5885;
})()
;
;})(args__$1,guard,assertion,expected))
});})(args__$1,guard,assertion,expected))
);
});

latte.overwrite.method.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.overwrite.method.cljs$lang$applyTo = (function (seq5880){
var G__5881 = cljs.core.first(seq5880);
var seq5880__$1 = cljs.core.next(seq5880);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5881,seq5880__$1);
});

latte.overwrite.property = (function latte$overwrite$property(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5890 = arguments.length;
var i__4731__auto___5891 = (0);
while(true){
if((i__4731__auto___5891 < len__4730__auto___5890)){
args__4736__auto__.push((arguments[i__4731__auto___5891]));

var G__5892 = (i__4731__auto___5891 + (1));
i__4731__auto___5891 = G__5892;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.overwrite.property.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.overwrite.property.cljs$core$IFn$_invoke$arity$variadic = (function (n,args){
var args__$1 = cljs.core.apply.cljs$core$IFn$_invoke$arity$2(cljs.core.hash_map,args);
var guard = cljs.core.cst$kw$guard.cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly(false));
var assertion = cljs.core.cst$kw$assertion.cljs$core$IFn$_invoke$arity$1(args__$1);
return latte.overwrite.assertions.overwriteProperty(cljs.core.name(n),((function (args__$1,guard,assertion){
return (function (super$){
return ((function (args__$1,guard,assertion){
return (function (){
var this$ = this;
var obj = (this$["_obj"]);
if(cljs.core.truth_((guard.cljs$core$IFn$_invoke$arity$1 ? guard.cljs$core$IFn$_invoke$arity$1(obj) : guard.call(null,obj)))){
return this$.assert((assertion.cljs$core$IFn$_invoke$arity$1 ? assertion.cljs$core$IFn$_invoke$arity$1(obj) : assertion.call(null,obj)),cljs.core.cst$kw$message.cljs$core$IFn$_invoke$arity$1(args__$1),cljs.core.cst$kw$negation.cljs$core$IFn$_invoke$arity$1(args__$1),cljs.core.clj__GT_js(obj));
} else {
return super$.apply(this$);
}
});
;})(args__$1,guard,assertion))
});})(args__$1,guard,assertion))
);
});

latte.overwrite.property.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.overwrite.property.cljs$lang$applyTo = (function (seq5888){
var G__5889 = cljs.core.first(seq5888);
var seq5888__$1 = cljs.core.next(seq5888);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5889,seq5888__$1);
});

