// Compiled by ClojureScript 1.10.516 {}
goog.provide('latte.add');
goog.require('cljs.core');
goog.require('kit.core');
latte.add.chai = (cljs.core.truth_(kit.core.module_system_QMARK_.call(null))?require("chai"):chai);
latte.add.assertions = (latte.add.chai["Assertion"]);
latte.add.method = (function latte$add$method(var_args){
var args__4736__auto__ = [];
var len__4730__auto___1582 = arguments.length;
var i__4731__auto___1583 = (0);
while(true){
if((i__4731__auto___1583 < len__4730__auto___1582)){
args__4736__auto__.push((arguments[i__4731__auto___1583]));

var G__1584 = (i__4731__auto___1583 + (1));
i__4731__auto___1583 = G__1584;
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
var G__1585__delegate = function (expectations){
var this$ = this;
var obj = (this$["_obj"]);
return this$.assert(cljs.core.apply.call(null,assertion,obj,expectations),new cljs.core.Keyword(null,"message","message",-406056002).cljs$core$IFn$_invoke$arity$1(args__$1),new cljs.core.Keyword(null,"negation","negation",-755634643).cljs$core$IFn$_invoke$arity$1(args__$1),expected.call(null,expectations),cljs.core.clj__GT_js.call(null,obj));
};
var G__1585 = function (var_args){
var expectations = null;
if (arguments.length > 0) {
var G__1586__i = 0, G__1586__a = new Array(arguments.length -  0);
while (G__1586__i < G__1586__a.length) {G__1586__a[G__1586__i] = arguments[G__1586__i + 0]; ++G__1586__i;}
  expectations = new cljs.core.IndexedSeq(G__1586__a,0,null);
} 
return G__1585__delegate.call(this,expectations);};
G__1585.cljs$lang$maxFixedArity = 0;
G__1585.cljs$lang$applyTo = (function (arglist__1587){
var expectations = cljs.core.seq(arglist__1587);
return G__1585__delegate(expectations);
});
G__1585.cljs$core$IFn$_invoke$arity$variadic = G__1585__delegate;
return G__1585;
})()
;})(args__$1,assertion,expected))
);
});

latte.add.method.cljs$lang$maxFixedArity = (1);

/** @this {Function} */
latte.add.method.cljs$lang$applyTo = (function (seq1580){
var G__1581 = cljs.core.first.call(null,seq1580);
var seq1580__$1 = cljs.core.next.call(null,seq1580);
var self__4717__auto__ = this;
return self__4717__auto__.cljs$core$IFn$_invoke$arity$variadic(G__1581,seq1580__$1);
});


//# sourceMappingURL=add.js.map
