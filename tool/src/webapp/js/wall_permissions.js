function WallPermissions(data) {

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
		if('wall.post.read.any' === data[i])
			this.postReadAny = true;
		else if('wall.post.create' === data[i])
			this.postCreate = true;
		else if('wall.post.delete.any' === data[i])
			this.postDeleteAny = true;
		else if('wall.post.delete.own' === data[i])
			this.postDeleteOwn = true;
		else if('wall.post.update.any' === data[i])
			this.postUpdateAny = true;
		else if('wall.post.update.own' === data[i])
			this.postUpdateOwn = true;
		else if('wall.comment.create' === data[i])
			this.commentCreate = true;
		else if('wall.comment.delete.any' === data[i])
			this.commentDeleteAny = true;
		else if('wall.comment.delete.own' === data[i])
			this.commentDeleteOwn = true;
		else if('wall.comment.update.any' === data[i])
			this.commentUpdateAny = true;
		else if('wall.comment.update.own' === data[i])
			this.commentUpdateOwn = true;
		else if('wall.modify.permissions' === data[i])
			this.modifyPermissions = true;
	}
}
