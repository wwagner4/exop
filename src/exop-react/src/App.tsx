import React from 'react';
import './App.css';
import Main from "./components/Main";
import config from "./config";

function App() {
    let url = config.exop_url
    if (url === undefined) throw Error("config 'exop_url' is not defined")
    return (
        <div className="App">
            <header className="App-header">
                <h1>exop</h1>
                <p className={"info-text1"}>Visualize planetary systems where exoplanets were discovered</p>
                <p className={"info-text1"}>Based on the data of the <a target="_blank" href={"http://www.openexoplanetcatalogue.com/"}>open exoplanet catalogue</a><br/>
                Author: <a target="_blank" href={"http://entelijan.net/"}>entelijan</a>
                </p>
                <Main url={url}/>
            </header>
        </div>
    );
}

export default App;
