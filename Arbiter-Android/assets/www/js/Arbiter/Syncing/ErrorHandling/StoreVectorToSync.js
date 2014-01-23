Arbiter.StoreVectorToSync = function(_map, _downloadOnly, _onSuccess){
	
	this.onSuccess = _onSuccess;
	
	this.map = _map;
	
	this.downloadOnly = _downloadOnly;
	
	this.layers = this.map.getLayersByClass("OpenLayers.Layer.Vector");

	this.index = -1;
	
	this.failedToStore = null;
};

Arbiter.StoreVectorToSync.prototype.pop = function(){
	
	if(++this.index < this.layers.length){
		
		var layer = this.layers[this.index];
		
		if(layer.name !== Arbiter.AOI){
			
			return layer;
		}
		
		return this.pop();
	}
	
	return undefined;
};

Arbiter.StoreVectorToSync.prototype.onComplete = function(){
	
	if(Arbiter.Util.funcExists(this.onSuccess)){
		this.onSuccess(this.failedToStore);
	}
};

Arbiter.StoreVectorToSync.prototype.addToFailed = function(failedId){
	
	if(Arbiter.Util.existsAndNotNull(failedId)){
		
		if(!Arbiter.Util.existsAndNotNull(this.failedToStore)){
			
			this.failedToStore = [];
		}
		
		this.failedToStore.push(failedId);
	}
};

Arbiter.StoreVectorToSync.prototype.startStore = function(){
	this.storeNext();
};

Arbiter.StoreVectorToSync.prototype.storeNext = function(){
	
	var olLayer = this.pop();
	
	if(olLayer !== undefined){
		this.store(olLayer);
	}else{
		this.onComplete();
	}
};

Arbiter.StoreVectorToSync.prototype.store = function(olLayer){
	
	var context = this;
	
	var key = Arbiter.Util.getLayerId(olLayer);
	
	var dataType = Arbiter.FailedSyncHelper.DATA_TYPES.VECTOR;
	var syncType = Arbiter.FailedSyncHelper.SYNC_TYPES.DOWNLOAD;
	
	var onFailure = function(e){
		
		context.addToFailed(key);
		
		context.storeNext();
	};
	
	Arbiter.FailedSyncHelper.insert(key, dataType, syncType, function(){
		
		if(context.downloadOnly !== true && context.downloadOnly !== "true"){
			syncType = Arbiter.FailedSyncHelper.SYNC_TYPES.UPLOAD;
			
			Arbiter.FailedSyncHelper.insert(key, dataType, syncType, function(){
				
				context.storeNext();
				
			}, function(e){
				
				onFailure(e);
			});
		}else{
			context.storeNext();
		}
		
	}, function(e){
		
		onFailure(e);
	});
};


