--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

create table cris_do_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_do_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_box2policygroup (box_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_box2policysingle (box_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_tab2policygroup (tab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_tab2policysingle (tab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_do_etab2policygroup (etab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_do_etab2policysingle (etab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_ou_etab2policygroup (etab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_ou_etab2policysingle (etab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_pj_etab2policygroup (etab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_pj_etab2policysingle (etab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_rp_etab2policygroup (etab_id number(10,0) not null, authorizedGroup varchar2(255 char));
create table cris_rp_etab2policysingle (etab_id number(10,0) not null, authorizedSingle varchar2(255 char));
create table cris_weperson (id number(10,0) not null, filter clob, primary key (id));
create table cris_wgroup (id number(10,0) not null, filter clob, primary key (id));
alter table jdyna_values add customPointer number(10,0);
alter table cris_do_box2policygroup add constraint FK_FQXYX09RDFU2FDLML08828XK8 foreign key (box_id) references cris_do_box;
alter table cris_do_box2policysingle add constraint FK_MJLADL11M2680HN8O4BTB4LLY foreign key (box_id) references cris_do_box;
alter table cris_do_tab2policygroup add constraint FK_J7D7VI6LJ4H5WICTL9SH6LMOT foreign key (tab_id) references cris_do_tab;
alter table cris_do_tab2policysingle add constraint FK_I6F2KS46TA2J5VS00ERQO2IP5 foreign key (tab_id) references cris_do_tab;
alter table cris_ou_box2policygroup add constraint FK_60GJY1OE2YRPM5180HFXAOP3L foreign key (box_id) references cris_ou_box;
alter table cris_ou_box2policysingle add constraint FK_DLLSGBTW3RAIF8EDKIDOEOI56 foreign key (box_id) references cris_ou_box;
alter table cris_ou_tab2policygroup add constraint FK_484L9Q8OJJIUK522QWTJ5C78W foreign key (tab_id) references cris_ou_tab;
alter table cris_ou_tab2policysingle add constraint FK_P3J0R630VYKJ5T6OD5YSD4IKK foreign key (tab_id) references cris_ou_tab;
alter table cris_pj_box2policygroup add constraint FK_RBDF00OP5AYM7RCHNQXTSC7BV foreign key (box_id) references cris_pj_box;
alter table cris_pj_box2policysingle add constraint FK_OL4361XB7GUETM6MLRLTOKEUR foreign key (box_id) references cris_pj_box;
alter table cris_pj_tab2policygroup add constraint FK_AMF69CHWWQ9KTP2FBJ0TAGT3A foreign key (tab_id) references cris_pj_tab;
alter table cris_pj_tab2policysingle add constraint FK_456MU7ALTF23L6U163L255ERC foreign key (tab_id) references cris_pj_tab;
alter table cris_rp_box2policygroup add constraint FK_PWXGXCDDBVE4X9H92IQTXXPFF foreign key (box_id) references cris_rp_box;
alter table cris_rp_box2policysingle add constraint FK_RITNNJKRJLP044YJO0TFPT19F foreign key (box_id) references cris_rp_box;
alter table cris_rp_tab2policygroup add constraint FK_AJNK574OOWMGPUD4O9KPCTLP foreign key (tab_id) references cris_rp_tab;
alter table cris_rp_tab2policysingle add constraint FK_7DME5A998B5CFFS2YNYK94L5D foreign key (tab_id) references cris_rp_tab;
alter table cris_do_etab2policygroup add constraint FK_NOTGNA4379IV6H6GH90VQVGG2 foreign key (etab_id) references cris_do_etab;
alter table cris_do_etab2policysingle add constraint FK_9C541J5OG0FN26AIIG2UD2K2D foreign key (etab_id) references cris_do_etab;
alter table cris_ou_etab2policygroup add constraint FK_CTYVOAR08KMYRKSR21FS3WJCH foreign key (etab_id) references cris_ou_etab;
alter table cris_ou_etab2policysingle add constraint FK_6CW0XRJNG41AEDD4MK76JCM39 foreign key (etab_id) references cris_ou_etab;
