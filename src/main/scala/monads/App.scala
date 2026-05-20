package monads

import domain.AppState

type App[A] = State[AppState, A]