import * as functions from 'firebase-functions';

const admin = require('firebase-admin');
admin.initializeApp();

const request = require('request');

export const getMapData = functions.https.onCall((data, context) => {
    return new Promise((resolve, reject) => {
        const now: Date = new Date();
        const year = now.getUTCFullYear();
        let month = now.getMonth().toString();

        if (month.length === 1) {
            month = '0' + month;
        }

        let day = now.getDate().toString();

        if (day.length === 1) {
            day = '0' + day;
        }

        const url = `http://homepages.inf.ed.ac.uk/stg/coinz/${year}/${month}/${day}/coinzmap.geojson`;

        console.log('Pulling in URL ', url);

        let ret: string = '';

        let getMyBody = (url, callback) => {
            request(url, (err, resp, body) => {
                if (!err) {
                    console.log('No error!');
                    callback(body);
                } else {
                    console.log('error', err);
                }
            });
        }

        function responseCallback(body) {
            console.log("in callback");
            resolve(body);
        }

        getMyBody(url, responseCallback);

    });
});
