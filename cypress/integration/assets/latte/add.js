// Compiled by ClojureScript 1.10.516 {}
goog.provide('latte.add');
goog.require('cljs.core');
goog.require('kit.core');
latte.add.chai = (cljs.core.truth_(kit.core.module_system_QMARK_.call(null))?require("chai"):chai);
latte.add.assertions = (latte.add.chai["Assertion"]);
latte.add.method = (function latte$add$method(var_args){
var args__4736__auto__ = [];
var len__4730__auto___5166 = arguments.length;
var i__4731__auto___5167 = (0);
while(true){
if((i__4731__auto___5167 < len__4730__auto___5166)){
args__4736__auto__.push((arguments[i__4731__auto___5167]));

var G__5168 = (i__4731__auto___5167 + (1));
i__4731__auto___5167 = G__5168;
continue;
} else {
}
break;
}

var argseq__4737__auto__ = ((((1) < args__4736__auto__.length))?(new cljs.core.IndexedSeq(args__4736__auto__.slice((1)),(0),null)):null);
return latte.add.method.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),argseq__4737__auto__);
});

latte.add.method.cljs$core$IFn$_invoke$arity$variadic = (function (k,args){
var args__$1 = cljs.core.apply.call(null,cljs.core.hash_map,args);
var assertion = new cljs.core.Keyword(null,"assertion","assertion",-1645134882).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.constantly.call(null,false));
var expected = new cljs.core.Keyword(null,"expected","expected",1583670997).cljs$core$IFn$_invoke$arity$2(args__$1,cljs.core.comp.call(null,cljs.core.clj__GT_js,cljs.core.first));
return latte.add.assertions.addMethod(cljs.core.name.call(null,k),((function (args__$1,assertion,expected){
return (function() { 
var G__5169__delegate = function (expectations){
var this$ = this;
var obj = (this$["_obj"]);
return this$.assert(cljs.core.apply.call(null,assertion,obj,expectations),new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(args__$1),new cljs.core.Keyword(null,"negation","negation",-755634643).cljs$core$IFn$_invoke$arity$1(args__$1),expected.call(null,expectations),cljs.core.clj__GT_js.call(null,obj));
};
var G__5169 = function (var_args){
var expectations = null;
if (arguments.length > 0) {
var G__5170__i = 0, G__5170__a = new Array(arguments.length -  0);
while (G__5170__i < G__5170__a.length) {G__5170__a[G__5170__i] = arguments[G__5170__i + 0]; ++G__5170__i;}
  expectations = new cljs.core.IndexedSeq(G__5170__a,0,null);
} 
return G__5169__delegate.call(this,expectations);};
G__5169.cljs$lang$maxFixedArity = 0;
G__5169.cljs$lang$applyTo = (function (arglist__5171){
var expectations = cljs.core.seq(arglist__5171);
return G__5169__delegate(expectations);
});
G__5169.cljs$core$IFn$_invoke$arity$variadic = G__5169__delegate;
return G__5169;
})()
;})(args__$1,assertion,expected))
);
});

latte.add.method.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.add.method.cljs$lang$applyTo = (function (seq5164){
var G__5165 = cljs.core.first.call(null,seq5164);
var seq5164__$1 = cljs.core.next.call(null,seq5164);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__5165,seq5164__$1);
});

