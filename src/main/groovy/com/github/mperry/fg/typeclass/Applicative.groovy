package com.github.mperry.fg.typeclass

import fj.F
import fj.F2
import fj.F2Functions
import fj.F3
import fj.Function
import groovy.transform.TypeChecked

/**
 * Created by MarkPerry on 26/06/2014.
 */
@TypeChecked
trait Applicative<App> extends Functor<App> {

	/**
	 * pure  :: a -> f a
	 */
	abstract <A> App<A> pure(A a)

	/**
	 * (<*>) :: f (a -> b) -> f a -> f b
	 */
	abstract <A, B> App<B> apply(App<F<A, B>> t1, App<A> t2)

	/**
	 * (<*) :: f a -> f b -> f a
	 */
	def <A, B> App<A> left(App<A> a1, App<B> a2) {
		a1
	}

	/**
	 * (*>) :: f a -> f b -> f b
	 */
	def <A, B> App<B> right(App<A> a1, App<B> a2) {
		a2
	}

	/**
	 * liftA :: Applicative f => (a -> b) -> f a -> f b
	 */
	def <A, B> App<B> liftA(F<A, B> f, App<A> a1) {
		apply(pure(f), a1)
	}

	/**
	 * liftA2 :: Applicative f => (a -> b -> c) -> f a -> f b -> f c
	 */
	def <A, B, C> App<C> liftA2(F2<A, B, C> f, App<A> apa, App<B> apb) {
		apply(fmap(F2Functions.curry(f), apa), apb)
	}

	/**
	 * liftA3 :: Applicative f => (a -> b -> c -> d) -> f a -> f b -> f c -> f d
	 */
	def <A, B, C, D> App<D> liftA3(F3<A, B, C, D> f, App<A> apa, App<B> apb, App<C> apc) {
		apply(apply(fmap(Function.curry(f), apa), apb), apc)
	}

}
