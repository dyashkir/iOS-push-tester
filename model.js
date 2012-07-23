var poll = function(){
  setTimeout('$.update();', 100000);
}


function Device(name, token) {
    var self = this;
    self.name = name;
    self.token = token;
}
// Overall viewmodel for this screen, along with initial state
function PushScreenViewModel() {
  var self = this;

  self.badge = ko.observable('0');
  self.actionButton = ko.observable('do stuff');
  self.messageBody = ko.observable('All ur base are belong to us');

  self.devices = ko.observableArray( []);
  self.selectedDevice = ko.observable();
  self.submitPush = function(elem){
    var data =  ko.toJSON(self);
    $.post('/messages', data, function(){
      console.log('worked post');
      }
    ); 
  };

  //update stuff

  $.update = function() {
    $.getJSON('/devices', function(data){
      self.devices.removeAll();
      data.forEach(function(item){
        self.devices.push(new Device(item.name, item.token));
      });
      poll();
    });
  };

  $.update();
}

ko.applyBindings(new PushScreenViewModel());
