function CoursewallPermissions(data) {

	if (!data) return;

	this.postReadAny = false;
	this.postCreate = false;
	this.postDeleteAny = false;
	this.postDeleteOwn = false;
	this.postUpdateAny = false;
	this.postUpdateOwn = false;
	this.commentCreate = false;
	this.commentDeleteAny = false;
	this.commentDeleteOwn = false;
	this.commentUpdateAny = false;
	this.commentUpdateOwn = false;
	this.modifyPermissions = false;

	for(var i=0,j=data.length;i<j;i++) {
		if('coursewall.post.read.any' === data[i])
			this.postReadAny = true;
		else if('coursewall.post.create' === data[i])
			this.postCreate = true;
		else if('coursewall.post.delete.any' === data[i])
			this.postDeleteAny = true;
		else if('coursewall.post.delete.own' === data[i])
			this.postDeleteOwn = true;
		else if('coursewall.post.update.any' === data[i])
			this.postUpdateAny = true;
		else if('coursewall.post.update.own' === data[i])
			this.postUpdateOwn = true;
		else if('coursewall.comment.create' === data[i])
			this.commentCreate = true;
		else if('coursewall.comment.delete.any' === data[i])
			this.commentDeleteAny = true;
		else if('coursewall.comment.delete.own' === data[i])
			this.commentDeleteOwn = true;
		else if('coursewall.comment.update.any' === data[i])
			this.commentUpdateAny = true;
		else if('coursewall.comment.update.own' === data[i])
			this.commentUpdateOwn = true;
		else if('coursewall.modify.permissions' === data[i])
			this.modifyPermissions = true;
	}
}
