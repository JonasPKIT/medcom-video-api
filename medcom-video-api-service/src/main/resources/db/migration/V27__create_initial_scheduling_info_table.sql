CREATE TABLE scheduling_info (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  uuid varchar(40) NOT NULL,  
  host_pin bigint(20),
  guest_pin bigint(20),
  vmravailable_before int NOT NULL,
  vmrstart_time datetime NOT NULL,
  max_participants int NOT NULL,
  end_meeting_on_end_time bit NOT NULL,
  uri_with_domain varchar(100) NOT NULL,
  uri_without_domain varchar(100) NOT NULL,
  scheduling_template_id bigint(20),
  provision_status varchar(30) NOT NULL,
  provision_timestamp datetime,
  provisionvmrid varchar(50),
  meetings_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY (uuid),
  UNIQUE KEY (uri_without_domain),
  FOREIGN KEY (meetings_id) REFERENCES meetings(id),
  FOREIGN KEY (scheduling_template_id) REFERENCES scheduling_template(id)
)