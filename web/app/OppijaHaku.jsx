import React from "react"
import ReactDOM from "react-dom"
import Bacon from "baconjs"
import Http from "./http"

const oppijatE = new Bacon.Bus();
const oppijaE = new Bacon.Bus();

const OppijaHakuBoksi = () =>
  (
    <div>
      <label>Opiskelija</label>
      <input onInput={(e) => oppijatE.push(e.target.value)}></input>
    </div>
  )

const OppijaHakutulokset = ({oppijat}) => {
  const oppijatElems = oppijat.map((oppija, i) =>
    <li key={i}>
      <a href="#" onClick={() => oppijaE.push(oppija)}>{oppija.etunimet} {oppija.sukunimi} {oppija.hetu}</a>
    </li>
  )
  return (
    <ul>
      {oppijatElems}
    </ul>
  )
}

export const OppijaHaku = ({oppijat}) => (
  <div className="oppija-haku">
    <OppijaHakuBoksi />
    <OppijaHakutulokset oppijat={oppijat}/>
  </div>
)

export const oppijatP = oppijatE.throttle(200)
  .flatMapLatest(q => Http.get(`/oppija?nimi=${q}`))
  .toProperty([])

export const oppijaP = Bacon.update(
  undefined,
  [oppijaE], (p, n) => n,
  [oppijatP.changes().filter((l) => l.length === 1).map(".0")], (p, n) => p ? p : n
)