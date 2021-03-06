import './polyfills.js'
import './style/main.less'

import React from 'react'
import ReactDOM from 'react-dom'
import Bacon from 'baconjs'
import {Error, TopLevelError, isTopLevel, handleError, errorP} from './Error.jsx'
import {Login, userP} from './Login.jsx'
import {contentP} from './router'
import {selectOppijaE, updateResultE} from './Oppija.jsx'
import {TopBar} from './TopBar.jsx'

// Stays at `true` for five seconds after latest saved change. Reset to `false` when another Oppija is selected.
const savedP = updateResultE.flatMapLatest(() => Bacon.once(true).concat((selectOppijaE.merge(Bacon.later(5000))).map(false))).toProperty(false).skipDuplicates()

const topBarP = Bacon.combineWith(userP, savedP, (user, saved) => <TopBar user={user} saved={saved} />)
const allErrorsP = errorP(Bacon.combineAsArray(contentP, savedP))

// Renderered Virtual DOM
const domP = Bacon.combineWith(topBarP, userP, contentP, allErrorsP, (topBar, user, content, error) =>
    <div>
      <Error error={error}/>
      {topBar}
      {
        isTopLevel(error)
          ? <TopLevelError status={error.httpStatus} text={error.text} />
          : ( user
            ? content
            : <Login />
          )
      }
    </div>
)

// Render to DOM
domP.onValue((component) => ReactDOM.render(component, document.getElementById('content')))

// Handle errors
domP.onError(handleError)