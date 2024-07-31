(ns for-scientists.cavity-physics
  "The second part of Wolframite for scientists. Here, we consider a real physics problem as a demonstration of how one might use Wolframite in practice."
  (:require
   [scicloj.kindly.v4.kind :as k]))

(defmacro eval->>
  "Extends the threading macro to automatically pass the result to wolframite eval."
  [& xs]
  `(->> ~@xs wl/eval))
(defmacro TeX->>
  "Extends the thread-last macro to automatically eval and prepare the expression for TeX display."
  [& xs]
  `(->> ~@xs TeX wl/eval k/tex))

(k/md "# Cavity Physics (Wolfram for scientists II)

I assume that you made it here from part I. Congratulations!

## Cavities?
Outside of the operating room, the most common notion of cavities is, what we might otherwise call, an optical resonator. One way of looking at it, although quantum mechanics makes this more difficult, is that an optical cavity is simply just a light trap, such that once light gets in, then it bounces around for a while before it leaves.

For demo purposes, our question is then 'how can we model this?' and 'how does the light intensity, inside and outside of the cavity, depend on some experimental variables'?

## Setup
Assuming a simple two-mirror setup, with independent reflectivities and
transmission, the system can be described as a ray oscillating between four key interfaces, as illustrated in the figure.")
(k/hiccup [:img {:src "notebooks/for_scientists/Cavity-MirrorScatteringLoss.png"}])

(k/md "
We must therefore consider the electric field at each interface, before solving for the intensity both inside and outside.

## Interfaces
Setting the incoming field amplitude to unity, then the value of the field at position one, after one round trip, is the sum of the transmission coefficient and the field travelling in the opposing direction e.g.
")

(def E1
  (w/+ 't1 (w/* 'r1 (w/- 'E4))))

(k/md "At position two, the same field has travelled the full length of the cavity, L, and so has picked up a change in phase,")
(def phi
  (w/* 'k 'L))
(k/md ", where k is the field wavenumber:")

(def E2
  (w/* E1 (w/Exp (w/* w/I phi))))

(k/md "At position three, the field undergoes reflection and so now carries an additional reflection coefficient as well as an extra π phase change.

Note how we are able to seamlessly mix Clojure and Wolfram expressions in these expressions.")

(def E3
  (w/* (w/- 'r2) E2))
(k/md "By position four, the field has travelled a further distance L and so is now equal to ")
(def E4
  (w/* E3 (w/Exp (w/* w/I phi))))
(TeX-> E4)

(k/md " ## Fields
 Using these expressions, we can now calculate the inner, transmitted and reflected fields of the cavity (with some subtle phase assumptions). Note how easy it is to, first of all, form an equation from a symbol 'E4 and the clojure variable E4, and, second of all, to rearrange the equation for a solution.

Substitution and simplification can then also be used to arrive at the transmission and reflection, respectively.
")

(def e4 (-> (w/== 'E4 E4)
            (w/Solve 'E4)
            w/First w/First))
(eval-> e4)

(def T (-> (w/x>> (w/* E2 't2)
                  e4)
           w/>>_<<))
(TeX-> T)

(def R (-> (w/+ (w/* E4 't1) 'r1)
           (w/x>> e4)
           w/>>_<<
           w/Together))
(TeX-> R)

(k/md "## Observables
 Taking the square of the fields (using the complex conjugate), gives the corresponding observables, i.e. things that can be actually measured, for the case of no loss.")

(def I4 (-> (w/x>> 'E4 e4) ||2
            w/++<_>
            w/>>_<<))
(TeX-> (w/== 'I4 (w/** (w/Abs 'E4) 2) I4))

(def Tsq (-> T ||2
             w/++<_>
             w/>>_<<))
(TeX-> (w/== (w/** 'T 2) Tsq))

(def Rsq (-> R ||2
             w/++<_>
             w/>>_<<
             w/Together))
(TeX-> (w/== (w/** 'R 2) Rsq))

(k/md "## Observables with loss
The above result is a perfectly reasonable, but simple, model. More realistically, we want to be able to understand mirrors that have losses, i.e. that don't just reflect or transmit light, but that actually absorb or 'waste' light.

This is one of the places that Wolfram really shines. Almost the entire engine is designed around replacement rules. And so we just have to define a substitution, which says that reflection + transmission + loss is equal to 1:
")

(def losses
  [(w/-> (w/** 'r1 2)
         (w/+ 1
              (w/- 'l1)
              (w/- (w/** 't1 2))))
   (w/-> (w/** 'r2 2)
         (w/+ 1
              (w/- 'l2)
              (w/- (w/** 't2 2))))])

(k/md "and substitute these into the equations. We're going to go a step further however...")

(k/md "## Approximations
Rather than just substitute the new rule into our expressions, we're going to take this opportunity to define some other replacement rules. Now so far, we've been fairly mathematically pure, but a lot of physics is about knowing when (and being able) to make good approximations.
")

(def approximations
  [(w/-> (w/* 'r1 'r2)
         (let [vars [(w/** 't1 2) (w/** 't2 2) 'l1 'l2]]
           (->  (w/x>> (w/* (w/√ (w/** 'r1 2))
                            (w/√ (w/** 'r2 2)))
                       losses)
                (w/Series ['t1 0 2]
                          ['t2 0 2]
                          ['l1 0 2]
                          ['l2 0 2])
                w/Normal
                w/<<_>>
                (w/x>> (mapv #(w/-> % (w/* 'temp %)) vars))
                (w/Series ['temp 0 1])
                w/Normal
                (w/x>> (w/-> 'temp 1)))))])
(TeX-> approximations)

(k/md "What's happening here is that we are simplifying the expression for r1*r2 by assuming that the transmission and loss for light going through a mirror is small. So small, that higher powers of these variabes are negligible. And so, we substitute the values into the expression, expand the functions in a power series and neglect any higher powers. The neglect is done by inserting ['big O' notation](https://mathworld.wolfram.com/Big-ONotation.html) and then restricting the series to a single power in that variable. As we want to do this for multiple variables, then we map over each one. This is a complicated mathematical procedure, but Wolframite allows us to do this quite concisely, apart from some necessary Wolfram datatype conversions (e.g. using the w/Normal function).

Note that Wolfram can also be used to define quite general approximations, using the 'Pattern' system. For example, let us assume that we want to expand Cos(x) as a polynomial when x is small. First, we need to know what the expansion is. We can find out by using Wolfram similarly to the above, i.e. ")

(eval-> (w/Series (w/Cos 'x)
                  ['x 0 2])
        w/Normal)

(k/md "Now that we know (or have remembered!) the form, we can create a general rule that is not limited to specific symbol definitions.")

(def small-angle
  (w/_> (w/Cos (w/Pattern 'x (w/Blank)))
        (w/+ 1 (w/* -1/2 (w/** 'x 2)))))

(k/md "The rule can now be named and used for any Cos function with any argument:")

(eval-> (w/x>> (w/Cos 'phi) small-angle))
(eval-> (w/x>> (w/Cos (w/+ 1 'phi)) small-angle))

(k/md "## Independent mirrors
With these approximations we can formulate our final expression for the optical intensity inside the cavity: ")
(TeX-> I4
       (w/x>> (w/-> (w/Cos (w/* 2 'k 'L))
                    (w/Cos 'phi)))
       (w/x>> approximations)
       (w/x>> (w/-> (w/** (w/* 'r1 'r2) 2)
                    (-> (w/x>> (w/* 'r1 'r2) approximations)
                        (w/** 2))))
       (w/x>> (conj losses small-angle))
       w/>_<
       (->> (w/== (w/** (w/Abs 'E4) 2))))

(k/md "# References
This tutorial was made by Thomas Clark, Jakub Holý and Daniel Slutsky. The cavity field derivation followed in the footsteps of one Francis 'Tito' Williams.")
