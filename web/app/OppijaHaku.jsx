import React from 'react'
import Bacon from 'baconjs'
import Http from './http'
import {navigateToOppija, navigateToUusiOppija} from './location'
import {oppijaP, oppijaStateP, Oppija} from './Oppija.jsx'

import { modelData } from './EditorModel.js'

const oppijaHakuE = new Bacon.Bus()

const acceptableQuery = (q) => q.length >= 3

const hakuTulosE = oppijaHakuE.debounce(500)
  .flatMapLatest(q => (acceptableQuery(q) ? Http.get(`/koski/api/oppija/search?query=${q}`) : Bacon.once([])).map((oppijat) => ({ results: oppijat, query: q })))

const henkilöP = oppijaP.map(oppija => modelData(oppija, 'henkilö'))

const henkilöE = henkilöP.toEventStream().filter(Bacon._.id)

export const oppijatP = Bacon.update(
  { query: '', results: [] },
  hakuTulosE, ((current, hakutulos) => hakutulos),
  henkilöE.filter(Bacon._.id), ((current, valittu) => current.results.filter((oppija) => oppija.oid === valittu.oid).length ? current : { query: '', results: [valittu] })
)

henkilöP.sampledBy(oppijatP.map('.results').changes(), (oppija, oppijat) => ({ oppija: oppija, oppijat: oppijat }))
  .filter(({oppija, oppijat}) => !oppija && oppijat.length === 1)
  .map('.oppijat.0')
  .onValue(navigateToOppija)

export const searchInProgressP = oppijaHakuE.filter(acceptableQuery).awaiting(oppijatP.mapError().changes()).throttle(200)

export const oppijaHakuContentP = Bacon.combineWith(oppijatP, searchInProgressP, oppijaStateP, (oppijat, searchInProgress, oppija) => {
  return (<div className='content-area'>
    <OppijaHaku oppijat={oppijat} valittu={modelData(oppija.valittuOppija, 'henkilö')} searching={searchInProgress}/>
    <Oppija oppija={oppija}/>
  </div>)
})


const OppijaHakuBoksi = React.createClass({
  render() {
    return (
      <div>
        <label>Opiskelija
          <input id='search-query' ref='query' placeholder='henkilötunnus, nimi tai oppijanumero' onInput={(e) => oppijaHakuE.push(e.target.value)}></input>
        </label>
        <hr></hr>
      </div>
    )
  },

  componentDidMount() {
    this.refs.query.focus()
  }
})

const OppijaHakutulokset = React.createClass({
  render() {
    const {oppijat, valittu} = this.props
    const oppijatElems = oppijat.results.map((o, i) => {
        const className = valittu ? (o.oid === valittu.oid ? 'selected' : '') : ''
        return (
          <li key={i} className={className}>
            <a onClick={this.selectOppija.bind(this, o)}>{o.sukunimi}, {o.etunimet} {o.hetu}</a>
          </li>
        )}
    )

    return oppijat.results.length > 0
      ? <ul> {oppijatElems} </ul>
      : oppijat.query.length > 2
        ? <div className='no-results'>Ei hakutuloksia</div>
        : null
  },

  selectOppija(oppija) {
    navigateToOppija(oppija)
  }
})

export const OppijaHaku = ({oppijat, valittu, searching}) => {
  const className = searching ? 'sidebar oppija-haku searching' : 'sidebar oppija-haku'
  return (
      <div className={className}>
        <OppijaHakuBoksi />
        <div className='hakutulokset'>
          <OppijaHakutulokset oppijat={oppijat} valittu={valittu}/>
          <div><a className='lisaa-oppija' onClick={navigateToUusiOppija}>Lisää oppija</a></div>
        </div>
      </div>
  )
}