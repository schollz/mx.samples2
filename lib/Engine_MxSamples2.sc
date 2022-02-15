// Engine_MxSamples2

// Inherit methods from CroneEngine
Engine_MxSamples2 : CroneEngine {

	// <mxsamples2>
    var mx;
	// </mxsamples2>

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		// <mxsamples2>
        mx=MxSamples(Server.default,100);

		this.addCommand("mx_note_on","sff", { arg msg;
			mx.noteOn(msg[1].asString,msg[2],msg[3]);
		});

		this.addCommand("mx_note_onfx","sffffffffff", { arg msg;

			mx.noteOnFX(msg[1].asString,msg[2],msg[3],
				//amp,pan,attack,decay,sustain,release,delaysend,reverbsend
				msg[4],
				msg[5],
				msg[6],
				msg[7],
				msg[8],
				msg[9],
				msg[10],
				msg[11],
			);
		});
        
		this.addCommand("mx_note_off","sf", { arg msg;
			mx.noteOff(msg[1].asString,msg[2]);
		});

		this.addCommand("mx_set","ssf", { arg msg;
			mx.setParam(msg[1].asString,msg[2].asString,msg[3]);
		});

        // </mxsamples2>
	}

	free {
		// <mxsamples2>
        mx.free;
        // </mxsamples2>
	}
}