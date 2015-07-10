ALTER TABLE jdyna_values ADD COLUMN booleanValue bool;
create table jdyna_widget_boolean (id int4 not null, showAsType varchar(255), checked bool, hideWhenFalse bool, primary key (id));
create table jdyna_widget_checkradio (id int4 not null, option4row integer, staticValues text, dropdown integer, primary key (id));


-- Table to mantain the potential match between item and rp --
CREATE TABLE potentialmatches
(
   potentialmatches_id integer, 
   item_id integer, 
   rp character varying(20), 
   pending integer,
    PRIMARY KEY (potentialmatches_id)
);
CREATE SEQUENCE potentialmatches_seq;
CREATE INDEX rp_idx
   ON potentialmatches (rp ASC NULLS LAST);
CREATE INDEX pending_idx
   ON potentialmatches (pending);
-- END potential matches --