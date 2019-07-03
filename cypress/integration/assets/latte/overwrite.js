// Compiled by ClojureScript 1.10.516 {}
goog.provide('latte.overwrite');
goog.require('cljs.core');
goog.require('kit.core');
latte.overwrite.chai = (cljs.core.truth_(kit.core.module_system_QMARK_.call(null))?require("chai"):chai);
latte.overwrite.assertions = (latte.overwrite.chai["Assertion"]);
latte.overwrite.method = (function latte$overwrite$method(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5234 = arguments.length;
var i__4731__auto___5235 = (0);
while(true){
if((i__4731__auto___5235 < len__4730__auto___5234)){
args__4736__auto__.push((arguments[i__4731__auto___5235]));

var G__5236 = (i__4731__auto___5235 + (1));
i__4731__auto___5235 = G__5236;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.overwrite.method.cljs$core$IFn$_invoke$arity$variadic = (function (n,args){
var args__$1 = cljs.core.apply.call(null,cljs.core.hash_map,args);
var guard = new cljs.core.Keyword(null,"guard","guard",-873147811).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly.call(null,false));
var assertion = new cljs.core.Keyword(null,"assertion","assertion",-1645134882).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly.call(null,false));
var expected = new cljs.core.Keyword(null,"expected","expected",1583670997).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.comp.call(null,cljs.core.clj__GT_js,cljs.core.first));
return latte.overwrite.assertions.overwriteMethod(cljs.core.name.call(null,n),((function (args__$1,guard,assertion,expected){
return (function (super$){
return ((function (args__$1,guard,assertion,expected){
return (function() { 
var G__5237__delegate = function (expectations){
var this$ = this;
var obj = (this$["_obj"]);
if(cljs.core.truth_(guard.call(null,obj))){
return this$.assert(cljs.core.apply.call(null,assertion,obj,expectations),new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(args__$1),new cljs.core.Keyword(null,"negation","negation",-755634643).cljs$core$IFn$_invoke$arity$1(args__$1),expected.call(null,expectations),cljs.core.clj__GT_js.call(null,obj));
} else {
return super$.apply(this$,cljs.core.into_array.call(null,expectations));
}
};
var G__5237 = function (var_args){
var expectations = null;
if (arguments.length > 0) {
var G__5238__i = 0, G__5238__a = new Array(arguments.length -  0);
while (G__5238__i < G__5238__a.length) {G__5238__a[G__5238__i] = arguments[G__5238__i + 0]; ++G__5238__i;}
  expectations = new cljs.core.IndexedSeq(G__5238__a,0,null);
} 
return G__5237__delegate.call(this,expectations);};
G__5237.cljs$lang$maxFixedArity = 0;
G__5237.cljs$lang$applyTo = (function (arglist__5239){
var expectations = cljs.core.seq(arglist__5239);
return G__5237__delegate(expectations);
});
G__5237.cljs$core$IFn$_invoke$arity$variadic = G__5237__delegate;
return G__5237;
})()
;
;})(args__$1,guard,assertion,expected))
});})(args__$1,guard,assertion,expected))
);
});

latte.overwrite.method.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.overwrite.method.cljs$lang$applyTo = (function (seq5232){
var G__5233 = cljs.core.first.call(null,seq5232);
var seq5232__$1 = cljs.core.next.call(null,seq5232);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5233,seq5232__$1);
});

latte.overwrite.property = (function latte$overwrite$property(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5242 = arguments.length;
var i__4731__auto___5243 = (0);
while(true){
if((i__4731__auto___5243 < len__4730__auto___5242)){
args__4736__auto__.push((arguments[i__4731__auto___5243]));

var G__5244 = (i__4731__auto___5243 + (1));
i__4731__auto___5243 = G__5244;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.overwrite.property.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.overwrite.property.cljs$core$IFn$_invoke$arity$variadic = (function (n,args){
var args__$1 = cljs.core.apply.call(null,cljs.core.hash_map,args);
var guard = new cljs.core.Keyword(null,"guard","guard",-873147811).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly.call(null,false));
var assertion = new cljs.core.Keyword(null,"assertion","assertion",-1645134882).cljs$core$IFn$_invoke$arity$1(args__$1);
return latte.overwrite.assertions.overwriteProperty(cljs.core.name.call(null,n),((function (args__$1,guard,assertion){
return (function (super$){
return ((function (args__$1,guard,assertion){
return (function (){
var this$ = this;
var obj = (this$["_obj"]);
if(cljs.core.truth_(guard.call(null,obj))){
return this$.assert(assertion.call(null,obj),new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(args__$1),new cljs.core.Keyword(null,"negation","negation",-755634643).cljs$core$IFn$_invoke$arity$1(args__$1),cljs.core.clj__GT_js.call(null,obj));
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
latte.overwrite.property.cljs$lang$applyTo = (function (seq5240){
var G__5241 = cljs.core.first.call(null,seq5240);
var seq5240__$1 = cljs.core.next.call(null,seq5240);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5241,seq5240__$1);
});

