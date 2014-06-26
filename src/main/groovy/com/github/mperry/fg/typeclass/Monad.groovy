package com.github.mperry.fg.typeclass

import fj.F
import fj.F2
import fj.F3
import fj.Unit
import fj.data.Stream
import groovy.transform.TypeChecked

/**
 * Created by MarkPerry on 26/06/2014.
 */
@TypeChecked
trait Monad<M> extends Applicative<M> {


	/**
	 * Implements Functor interface using Monad combinators
	 * fmap :: (a -> b) -> f a -> f b
	 */
	def <A, B> M<B> fmap(F<A, B> f, M<A> ma) {
		liftM(ma, f)
	}

	/**
	 * Implements Applicative.pure using Monad combinators
	 * pure  :: a -> f a
	 */
	def <A> M<A> pure(A a) {
		unit(a)
	}

	/**
	 * Implements Applicative.apply using Monad combinators
	 * (<*>) :: f (a -> b) -> f a -> f b
	 */
	def <A, B> M<B> apply(M<F<A, B>> t1, M<A> t2) {
		ap(t2, t1)
	}

	/**
	 * Sequentially compose two actions, passing any value produced by the first as
	 * an argument to the second.
	 * (>>=) :: forall a b. m a -> (a -> m b) -> m b
	 */
	abstract <A, B> M<B> flatMap(M<A> ma, F<A, M<B>> f)

	/**
	 * Inject a value into the monadic type.
	 * return :: a -> m a
	 */
	abstract <B> M<B> unit(B b)

	/**
	 * Returns a function representing unit
	 */
	def <B> F<B, M<B>> unit() {
		def t = this;
		{ B b ->
			t.unit(b)
		} as F
	}

	/**
	 * The join function is the conventional monad join operator. It is used to remove
	 * one level of monadic structure, projecting its bound argument into the outer level.
	 * join :: Monad m => m (m a) -> m a
	 */
	def <A> M<A> join(M<M<A>> mma) {
		flatMap(mma, {M<A> ma -> ma} as F)
	}

	def <A, B> M<B> map(M<A> ma, F<A, B> f) {
		def t = this;
		flatMap(ma, { A a ->
			t.unit(f.f(a))
		} as F)
	}

	def <A, B, C> M<C> map2(M<A> ma, M<B> mb, F2<A, B, C> f) {
		def t = this;
		flatMap(ma, { A a -> t.map(mb, { B b -> f.f(a, b)} as F)} as F)
	}

	def <A, B> M<B> to(M<A> ma, B b) {
		map(ma, { A a -> b } as F)
	}

	def <A> M<Unit> skip(M<A> ma) {
		to(ma, Unit.unit())
	}

	/**
	 * The foldM function is analogous to foldl, except that its result is encapsulated
	 * in a monad. Note that foldM works from left-to-right over the list arguments.
	 * This could be an issue where (>>) and the `folded function' are not commutative.
	 * foldM :: Monad m => (a -> b -> m a) -> a -> [b] -> m a
	 * Arguments are in different order
	 */
	def <A, B> M<B> foldM(Stream<A> s, B b, F2<B, A, M<B>> f) {
		if (s.empty) {
			unit(b)
		} else {
			def current = this
			def h = s.head()
			def t = s.tail()._1()
			def newF = { B bb -> current.foldM(t, bb, f)} as F
			def m = f.f(b, h)
			flatMap(m, newF)
		}
	}

	def <A, B> M<B> foldM(List<A> s, B b, F2<B, A, M<B>> f) {
		if (s.empty) {
			unit(b)
		} else {
			def current = this
			def h = s.head()
			def t = s.tail()
			def newF = { B bb -> current.foldM(t, bb, f)} as F
			def m = f.f(b, h)
			flatMap(m, newF)
		}
	}


	/**
	 * Like foldM, but discards the result.
	 * foldM_ :: Monad m => (a -> b -> m a) -> a -> [b] -> m ()
	 */
	def <A, B> M<Unit> foldM_(Stream<A> s, B b, F2<B, A, M<B>> f) {
		skip(foldM(s, b, f))
	}

	def <A, B> M<Unit> foldM_(List<A> s, B b, F2<B, A, M<B>> f) {
		skip(foldM(s, b, f))
	}


	/**
	 * Evaluate each action in the sequence from left to right, and collect the results.
	 * @param list
	 * @return
	 */
	def <A> M<List<A>> sequence(List<M<A>> list) {

		def current = this
		def k2 = { M<List<A>> acc, M<A> ma ->
			current.flatMap(acc, { List<A> xs ->
				current.map(ma, { A x ->
					ListOps.plus(xs, x)
//					xs + [x]
				} as F)
			} as F)
		}
		list.inject(unit([]), k2)
	}

	/**
	 * Map each element of a structure to an action, evaluate these actions from left to right
	 * and collect the results.
	 * @param list
	 * @param f
	 * @return
	 */
	def <A, B> M<List<B>> traverse(List<A> list, F<A, M<B>> f) {
		def current = this;
//		(M<List<B>>)
		def f2 = { M<List<B>> acc, A a ->
			current.flatMap(acc, { List<B> bs ->
				def mb = f.f(a)
				current.map(mb, { B b ->
					ListOps.plus(bs, b)
//					bs + [b]
				} as F)
			} as F)
		} //as F2
		list.inject(unit([]), f2)
//		list.inject(unit([]), { M<List<B>> acc, A a ->
//			current.flatMap(acc, { List<B> bs ->
//				def mb = f.f(a)
//				current.map(mb, { B b ->
//					ListOps.plus(bs, b)
////					bs + [b]
//				} as F)
//			} as F)
//		} as F2)
	}

	/**
	 * replicateM n act performs the action n times, gathering the results.
	 * replicateM :: Monad m => Int -> m a -> m [a] Source
	 */
	def <A> M<List<A>> replicateM(Integer n, M<A> ma) {
		sequence((1..n).collect { Integer i -> ma } )
	}

	/**
	 * Right-to-left Kleisli composition of monads. (>=>), with the arguments flipped
	 * http://hackage.haskell.org/package/base-4.6.0.1/docs/Control-Monad.html#v:-60--61--60-
	 * @param l
	 * @param f
	 * @param g
	 * @return
	 */
	def <A, B, C> F<A, M<C>> compose(F<B, M<C>> f, F<A, M<B>> g) {
		def current = this;
		{ A a ->
			current.flatMap(g.f(a), f)
		} as F
	}

	/**
	 * This generalizes the list-based filter function.
	 * Arguments flipped compared to Haskell representation
	 * filterM :: Monad m => (a -> m Bool) -> [a] -> m [a]
	 */
	def <A> M<List<A>> filterM(List<A> list, F<A, M<Boolean>> f) {
		def current = this
		if (list.empty) {
			unit([])
		} else {
			def h = list.head()
			def mb = f.f(h)
			flatMap(mb, { Boolean b ->
				def mList = current.filterM(list.tail(), f)
				current.map(mList, { List<A> listAs ->
					List<A> hList = [h]
//					b ? [h] + listAs : listAs
//					b ? hList + listAs : listAs
					b ? ListOps.plus(h, listAs) : listAs
				} as F)
			} as F)
		}
	}

	/**
	 * Conditional execution of monadic expressions. For example,
	 * when debug (putStr "Debugging\n")
	 * will output the string Debugging\n if the Boolean value debug is True, and
	 * otherwise do nothing.
	 * when :: Monad m => Bool -> m () -> m ()
	 */
	def M<Unit> when(Boolean b, M<Unit> m) {
		b ? m : unit(Unit.unit())
	}

	/**
	 * The reverse of when.
	 * unless :: Monad m => Bool -> m () -> m () Source
	 */
	def M<Unit> unless(Boolean b, M<Unit> m) {
		when(!b, m)
	}

	/**
	 * Promote a function to a monad.
	 * liftM :: Monad m => (a1 -> r) -> m a1 -> m r
	 */
	def <A, B> M<B> liftM(M<A> ma, F<A, B> f) {
		map(ma, { A a ->
			f.f(a)
		} as F)
	}

	/**
	 * Promote a function to a monad, scanning the monadic arguments from left to
	 * right. For example,
	 * liftM2 (+) [0,1] [0,2] = [0,2,1,3]
	 * liftM2 (+) (Just 1) Nothing = Nothing
	 * liftM2 :: Monad m => (a1 -> a2 -> r) -> m a1 -> m a2 -> m r
	 */
	def <A, B, R> M<R> liftM2(M<A> ma, M<B> mb, F2<A, B, R> f) {
		def current = this
		flatMap(ma, { A a ->
			current.map(mb, { B b ->
				f.f(a, b)
			} as F)
		} as F)
	}

	/**
	 * Promote a function to a monad, scanning the monadic arguments from left to
	 * right (cf. liftM2).
	 * liftM3 :: Monad m => (a1 -> a2 -> a3 -> r) -> m a1 -> m a2 -> m a3 -> m r
	 */
	def <A, B, C, R> M<R> liftM3(M<A> ma, M<B> mb, M<C> mc, F3<A, B, C, R> f) {
		def current = this
		flatMap(ma, { A a ->
			current.flatMap(mb, { B b ->
				current.map(mc, { C c ->
					f.f(a, b, c)
				} as F)
			} as F)
		} as F)
	}

	/**
	 * In many situations, the liftM operations can be replaced by uses of ap, which
	 * promotes function application.
	 * return f `ap` x1 `ap` ... `ap` xn
	 * is equivalent to
	 * liftMn f x1 x2 ... xn
	 * ap :: Monad m => m (a -> b) -> m a -> m b Source
	 */
	def <A, B> M<B> ap(M<A> ma, M<F<A, B>> mf) {
		def current = this
		flatMap(mf, { F<A, B> f ->
			current.map(ma, { A a ->
				f.f(a)
			} as F)
		} as F)
	}


}
