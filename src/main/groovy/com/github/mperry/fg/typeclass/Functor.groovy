package com.github.mperry.fg.typeclass

import groovy.transform.TypeChecked

import fj.F

/**
 * Created by MarkPerry on 26/06/2014.
 */
@TypeChecked
trait Functor<Fun> {

	abstract <A, B> Fun<B> fmap(F<A, B> f, Fun<A> fa)

}
