commons.sakai = {

    jqueryImport: /<script type="text\/javascript" src="\/profile2-tool\/javascript\/jquery-[\w\.]*\.js">\s*<\/script>/,
	getProfileMarkup: function (userId) {

		var profile = '';

		jQuery.ajax( {
	       	url: "/direct/profile/" + userId + "/formatted",
	       	dataType: "html",
	       	async: false,
			cache: false,
		   	success: function (p) {

                if(p.match(this.jqueryImport)) {
                    p = p.replace(this.jqueryImport, '');
                }

				profile = p;
			},
			error : function (xmlHttpRequest, textStatus, error) {
				//alert("Failed to get profile markup. Status: " + textStatus + ". Error: " + error);
			}
	   	});

		return profile;
	}
};
